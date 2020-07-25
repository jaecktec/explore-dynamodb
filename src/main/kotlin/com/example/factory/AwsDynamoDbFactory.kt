package com.example.factory

import io.micronaut.context.annotation.Bean
import io.micronaut.context.annotation.Factory
import io.micronaut.context.annotation.Value
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient
import java.net.URI

@Factory
class AwsDynamoDbFactory {

    @Value("\${dynamo.endpoint}")
    private lateinit var dynamoEndpoint: String

    @Value("\${dynamo.table}")
    private lateinit var dynamoTable: String

    @Bean
    fun tableNameProvider() = object: TableNameProvider{
        override fun tableName(): String = dynamoTable
    }

    @Bean
    fun dynamoDbClient(): DynamoDbAsyncClient {
        return DynamoDbAsyncClient
            .builder()
            .credentialsProvider(
                StaticCredentialsProvider.create(
                    AwsBasicCredentials.create("dummy-key", "dummy-secret")
                )
            )
            .region(Region.US_WEST_2)
            .endpointOverride(URI.create(dynamoEndpoint))
            .build()!!
    }
}

interface TableNameProvider {
    fun tableName(): String
}
