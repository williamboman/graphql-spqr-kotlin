# graphql-spqr-kotlin

[![CircleCI](https://circleci.com/gh/williamboman/graphql-spqr-kotlin.svg?style=svg)](https://circleci.com/gh/williamboman/graphql-spqr-kotlin)

> Kotlin compatibility library for [graphql-spqr](https://github.com/leangen/graphql-spqr).

This library aims to make it possible to seamlessly use graphql-spqr together with Kotlin. **Do not use this in production code, it's a POC.**

## Install

```xml
<dependencies>
    <dependency>
        <groupId>com.williamboman.graphql</groupId>
        <artifactId>spqr-kotlin</artifactId>
        <version>0.10.0</version>
    </dependency>
</dependencies>
```

## Usage

Please refer to https://github.com/leangen/graphql-spqr for information on how to use graphql-spqr.

```kt
import com.github.williamboman.graphql.transformer.KotlinTypesSchemaTransformer
import graphql.schema.GraphQLSchema
import io.leangen.graphql.GraphQLSchemaGenerator
import io.leangen.graphql.annotations.GraphQLIgnore
import io.leangen.graphql.annotations.GraphQLMutation
import io.leangen.graphql.annotations.GraphQLQuery
import io.leangen.graphql.annotations.types.GraphQLType

@GraphQLType(name = "User")
data class User(
    val name: String,
    val isAdmin: Boolean,
    @get:GraphQLIgnore
    val internalField: String
)

@GraphQLType(name = "UserInput")
data class UserInput(
    val firstname: String,
    val lastname: String,
    val middlename: String?
)

class UserResolver {

    @GraphQLQuery(name = "user")
    fun getUser(id: Int!): User? = getUser(id)

    @GraphQLQuery(name = "users")
    fun getUsers(): List<User> = getUsers()

    @GraphQLQuery(name = "nullableUsers")
    fun getNullableUsers(): List<User?> = getNullableUsers()

    @GraphQLMutation(name = "createUser")
    fun createUser(input: UserInput): User = createUser(input)

}

val graphqlSchema = GraphQLSchemaGenerator()
    .withOperationsFromSingletons(UserResolver())
    .withSchemaTransformers(KotlinTypesSchemaTransformer())
    .generate()

/**
   The above will produce the following schema:

   type User {
        name: String!
        isAdmin: Boolean!
   }

   input UserInput {
        firstname: String!
        lastname: String!
        middlename: String
   }

   type Query {
        user(id: Int!): User
        users: [User!]!
        nullableUsers: [User]!
   }

   type Mutation {
        createUser(input: UserInput!): User!
   }
```

## Configuration

To make it possible to use annotations for input arguments you need to ensure set `javaParameters=true` in your Kotlin compilation configuration.

```xml
<configuration>
    <javaParameters>true</javaParameters>
</configuration>
```

## Limitations

Due to https://youtrack.jetbrains.com/issue/KT-13228 annotations such as `@GraphQLId` does not work.
