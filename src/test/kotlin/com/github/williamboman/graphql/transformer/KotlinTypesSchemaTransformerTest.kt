package com.github.williamboman.graphql.transformer

import com.github.williamboman.graphql.TestSchemaGenerator
import graphql.Scalars
import graphql.schema.GraphQLInputObjectType
import graphql.schema.GraphQLList
import graphql.schema.GraphQLModifiedType
import graphql.schema.GraphQLNonNull
import graphql.schema.GraphQLObjectType
import graphql.schema.GraphQLType
import io.leangen.graphql.GraphQLSchemaGenerator
import io.leangen.graphql.annotations.GraphQLId
import io.leangen.graphql.annotations.GraphQLQuery
import org.junit.Ignore
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

fun unwrapType(type: GraphQLType?) = when (type) {
    is GraphQLModifiedType -> type.wrappedType
    else -> type
}

data class Input(
    val id: @GraphQLId String,
    val nonNullable: String,
    val nullable: Int?
)

class Service {
    @GraphQLQuery
    fun nullableBoolean(): Boolean? = null

    @GraphQLQuery
    fun nonNullableString(): String = ""

    @GraphQLQuery
    fun nonNullableListWithNullableItems(): List<String?> = listOf()

    @GraphQLQuery
    fun nonNullableListWithNonNullableItems(): List<String> = listOf()

    @GraphQLQuery
    fun nullableArg(arg: Int?): Boolean = true

    @GraphQLQuery
    fun nonNullableArg(arg: Int): Boolean = true

    @GraphQLQuery
    fun createInput(input: Input): Boolean = true

    @GraphQLQuery
    fun graphqlIdArg(@GraphQLId id: String): Boolean = true

    @GraphQLQuery
    fun input(): Input = Input("foo", "bar", 0)

    @GraphQLQuery
    fun id(): @GraphQLId String = ""
}

class KotlinTypesSchemaTransformerTest {

    private val schemaGenerator: GraphQLSchemaGenerator
        get() =
            TestSchemaGenerator()
                .withSchemaTransformers(KotlinTypesSchemaTransformer())
                .withOperationsFromSingleton(Service())

    @Test
    fun `should transform nullable`() {
        val schema = schemaGenerator.generate()

        assertEquals(Scalars.GraphQLBoolean, schema.queryType.getFieldDefinition("nullableBoolean").type)
    }

    @Test
    fun `should transform non-null field`() {
        val schema = schemaGenerator.generate()
        val graphqlType = schema.queryType.getFieldDefinition("nonNullableString").type

        assertTrue("nonNullableString should be an instance of GraphQLNonNull") {
            graphqlType is GraphQLNonNull
        }
        assertEquals(Scalars.GraphQLString, unwrapType(graphqlType))
    }

    @Test
    fun `should transform non-null list of nullable items`() {
        val schema = schemaGenerator.generate()
        val graphqlType = schema.queryType.getFieldDefinition("nonNullableListWithNullableItems").type

        with(graphqlType) {
            assertTrue("nonNullableListWithNullableItems should be an instance of GraphQLNonNull") {
                this is GraphQLNonNull
            }

            with(unwrapType(graphqlType)) {
                assertTrue("nonNullableListWithNullableItems.wrappedType should be an instance of GraphQLList") {
                    this is GraphQLList
                }

                with(unwrapType(this)) {
                    assertEquals(Scalars.GraphQLString, this)
                }
            }
        }
    }

    @Test
    fun `should transform non-null list of non-nullable items`() {
        val schema = schemaGenerator.generate()
        val graphqlType = schema.queryType.getFieldDefinition("nonNullableListWithNonNullableItems").type

        with(graphqlType) {
            assertTrue("nonNullableListWithNonNullableItems should be an instance of GraphQLNonNull") {
                this is GraphQLNonNull
            }

            with(unwrapType(this)) {
                assertTrue("nonNullableListWithNonNullableItems.wrappedType should be an instance of GraphQLList") {
                    this is GraphQLList
                }

                with(unwrapType(this)) {
                    assertTrue("nonNullableListWithNonNullableItems.wrappedType.wrappedType should be an instance of GraphQLNonNull") {
                        this is GraphQLNonNull
                    }

                    with(unwrapType(this)) {
                        assertEquals(Scalars.GraphQLString, this)
                    }
                }
            }
        }
    }

    @Test
    fun `should transform nullable argument`() {
        val schema = schemaGenerator.generate()

        val graphqlType = schema.queryType.getFieldDefinition("nullableArg").arguments[0].type

        assertEquals(Scalars.GraphQLInt, graphqlType)
    }

    @Test
    fun `should transform non-nullable argument`() {
        val schema = schemaGenerator.generate()

        val graphqlType = schema.queryType.getFieldDefinition("nonNullableArg").arguments[0].type

        with(graphqlType) {
            assertTrue("nullableArg.arguments[0] should be an instance of GraphQLNonNull") {
                this is GraphQLNonNull
            }

            with(unwrapType(this)) {
                assertEquals(Scalars.GraphQLInt, this)
            }
        }
    }

    @Test
    fun `should transform input fields`() {
        val schema = schemaGenerator.generate()

        val inputArgument = schema.queryType.getFieldDefinition("createInput").arguments[0]
        with(unwrapType(inputArgument.type)) {
            val inputObjectType = this as GraphQLInputObjectType

            with(inputObjectType.getField("nullable").type) {
                assertEquals(Scalars.GraphQLInt, this)
            }

            with(inputObjectType.getField("nonNullable").type) {
                assertTrue("input.arguments[0].nonNullable should be an instance of GraphQLNonNull") {
                    this is GraphQLNonNull
                }

                with(unwrapType(this)) {
                    assertEquals(Scalars.GraphQLString, this)
                }
            }
        }
    }

    @Test
    fun `should transform GraphQL ID argument types`() {
        val schema = schemaGenerator.generate()

        val inputArgument = schema.queryType.getFieldDefinition("graphqlIdArg").arguments[0]

        assertEquals(GraphQLNonNull(Scalars.GraphQLID), inputArgument.type)
    }

    @Test
    @Ignore("https://youtrack.jetbrains.com/issue/KT-13228")
    fun `should transform GraphQL ID field types`() {
        val schema = schemaGenerator.generate()

        val graphqlType = schema.queryType.getFieldDefinition("input").type

        with (unwrapType(graphqlType)) {
            val objectType = this as GraphQLObjectType
            val idField = objectType.getFieldDefinition("id")
            assertEquals(GraphQLNonNull(Scalars.GraphQLID), idField.type)
        }

    }

    @Test
    @Ignore("https://youtrack.jetbrains.com/issue/KT-13228")
    fun `should transform GraphQL ID root query return type`() {
        val schema = schemaGenerator.generate()

        val graphqlType = schema.queryType.getFieldDefinition("id").type
        assertEquals(GraphQLNonNull(Scalars.GraphQLID), graphqlType)
    }
}
