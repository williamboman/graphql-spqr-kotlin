package com.github.williamboman.graphql

import io.leangen.graphql.GraphQLSchemaGenerator
import io.leangen.graphql.metadata.strategy.query.DefaultOperationBuilder

class TestSchemaGenerator : GraphQLSchemaGenerator() {
    init {
        withBasePackages("com.github.williamboman.graphql")
        withOperationBuilder(DefaultOperationBuilder(DefaultOperationBuilder.TypeInference.LIMITED))
    }
}
