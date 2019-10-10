# graphql-spqr-kotlin

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

```java
import com.github.williamboman.graphql.transformer.KotlinTypesSchemaTransformer;
import graphql.schema.GraphQLSchema;
import io.leangen.graphql.GraphQLSchemaGenerator;

GraphQLSchema graphqlSchema = new GraphQLSchemaGenerator()
    .withSchemaTransformers(new KotlinTypesSchemaTransformer())
    .generate();
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
