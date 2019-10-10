package com.github.williamboman.graphql.transformer

import graphql.Scalars
import graphql.schema.*
import io.leangen.graphql.annotations.GraphQLId
import io.leangen.graphql.generator.BuildContext
import io.leangen.graphql.generator.OperationMapper
import io.leangen.graphql.generator.mapping.SchemaTransformer
import io.leangen.graphql.metadata.InputField
import io.leangen.graphql.metadata.Operation
import io.leangen.graphql.metadata.OperationArgument
import io.leangen.graphql.metadata.TypedElement
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.lang.reflect.Parameter
import kotlin.reflect.KCallable
import kotlin.reflect.KParameter
import kotlin.reflect.full.instanceParameter
import kotlin.reflect.jvm.jvmErasure
import kotlin.reflect.jvm.kotlinFunction
import kotlin.reflect.jvm.kotlinProperty

private fun Class<*>.isKotlinClass(): Boolean {
    return this.declaredAnnotations.any {
        it.annotationClass.qualifiedName == "kotlin.Metadata"
    }
}

private fun GraphQLFieldDefinition.transformIfScalarAnnotation(operation: Operation): GraphQLFieldDefinition =
    operation
        .takeIf { it.typedElement.isGraphQLId() }
        ?.let {
            this.transform { builder -> builder.type(Scalars.GraphQLID) }
        }
        ?: this

private fun GraphQLInputObjectField.transformIfScalarAnnotation(inputField: InputField): GraphQLInputObjectField =
    inputField
        .takeIf { it.typedElement.isGraphQLId() }
        ?.let {
            this.transform { builder -> builder.type(Scalars.GraphQLID) }
        }
        ?: this

private fun GraphQLArgument.transformIfScalarAnnotation(operationArgument: OperationArgument): GraphQLArgument =
    operationArgument
        .takeIf { it.typedElement.isGraphQLId() }
        ?.let {
            this.transform { builder -> builder.type(Scalars.GraphQLID) }
        }
        ?: this

private fun KCallable<*>.takeIfNonNull(field: GraphQLType): KCallable<*>? {
    return this
        .takeIf { it.instanceParameter?.type?.jvmErasure?.java?.isKotlinClass() == true }
        ?.takeUnless { it.returnType.isMarkedNullable }
        ?.takeUnless { field is GraphQLNonNull }
}

private fun KParameter.takeIfNonNull(argument: GraphQLType): KParameter? {
    return this
        .takeUnless { it.type.isMarkedNullable }
        ?.takeUnless { argument is GraphQLNonNull }
}

private fun <T : GraphQLType> markNonNullable(
    fieldType: GraphQLType,
    kCallable: KCallable<*>,
    setFieldType: (type: T) -> Unit
) {
    if (fieldType is GraphQLList) {
        val wrappedType = fieldType.wrappedType
        if (kCallable.returnType.arguments[0]?.type?.isMarkedNullable == false) {
            setFieldType(GraphQLNonNull(GraphQLList.list(GraphQLNonNull(wrappedType))) as T)
        } else {
            setFieldType(GraphQLNonNull(fieldType) as T)
        }
    } else {
        setFieldType(GraphQLNonNull(fieldType) as T)
    }
}

private fun GraphQLFieldDefinition.markNonNullable(kCallable: KCallable<*>) =
    this.transform { builder ->
        markNonNullable<GraphQLOutputType>(this.type, kCallable) {
            builder.type(it)
        }
    }

private fun GraphQLInputObjectField.markNonNullable(kCallable: KCallable<*>) =
    this.transform { builder ->
        markNonNullable<GraphQLInputType>(this.type, kCallable) {
            builder.type(it)
        }
    }

private fun TypedElement.coalesceKCallable() =
    this.elements.filterIsInstance<Field>().firstOrNull()?.kotlinProperty
        ?: this.elements.filterIsInstance<Method>().firstOrNull()?.kotlinFunction

private fun TypedElement.isGraphQLId(): Boolean =
    this.isAnnotationPresent(GraphQLId::class.java)

private fun OperationArgument.findParameter(argument: GraphQLArgument): KParameter? =
    ((this.typedElement.element as? Parameter)?.declaringExecutable as? Method)?.kotlinFunction
        ?.takeIf { it.instanceParameter?.type?.jvmErasure?.java?.isKotlinClass() == true }
        ?.let { it.parameters }
        ?.firstOrNull { it.name == argument.name }

class KotlinTypesSchemaTransformer : SchemaTransformer {

    override fun transformField(
        field: GraphQLFieldDefinition,
        operation: Operation,
        operationMapper: OperationMapper,
        buildContext: BuildContext
    ): GraphQLFieldDefinition {
        val kotlinCallable = operation.typedElement.coalesceKCallable()
        val transformedField = field.transformIfScalarAnnotation(operation)

        return kotlinCallable
            ?.takeIfNonNull(transformedField.type)
            ?.let { transformedField.markNonNullable(it) }
            ?: super.transformField(field, operation, operationMapper, buildContext)
    }

    override fun transformInputField(
        field: GraphQLInputObjectField,
        inputField: InputField,
        operationMapper: OperationMapper,
        buildContext: BuildContext
    ): GraphQLInputObjectField {
        val kotlinCallable = inputField.typedElement.coalesceKCallable()
        val transformedField = field.transformIfScalarAnnotation(inputField)

        return kotlinCallable
            ?.takeIfNonNull(transformedField.type)
            ?.let { transformedField.markNonNullable(it) }
            ?: super.transformInputField(field, inputField, operationMapper, buildContext)
    }

    override fun transformArgument(
        argument: GraphQLArgument,
        operationArgument: OperationArgument,
        operationMapper: OperationMapper,
        buildContext: BuildContext
    ): GraphQLArgument {
        val transformedArgument = argument.transformIfScalarAnnotation(operationArgument)

        return operationArgument
            .findParameter(transformedArgument)
            ?.takeIfNonNull(transformedArgument.type)
            ?.let { transformedArgument.transform { builder -> builder.type(GraphQLNonNull.nonNull(transformedArgument.type)) } }
            ?: super.transformArgument(transformedArgument, operationArgument, operationMapper, buildContext)
    }
}
