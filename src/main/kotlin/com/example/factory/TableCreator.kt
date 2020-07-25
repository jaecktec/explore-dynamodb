package com.example.factory

import com.example.factory.TableNameProvider
import io.micronaut.context.annotation.Requires
import io.micronaut.context.event.ApplicationEventListener
import io.micronaut.context.event.StartupEvent
import org.slf4j.LoggerFactory
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement
import software.amazon.awssdk.services.dynamodb.model.KeyType
import software.amazon.awssdk.services.dynamodb.model.ProvisionedThroughput
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType
import javax.inject.Singleton

@Singleton
@Requires(property = "dynamo.create-table", value = "true")
class TableCreator(
    private val dynamoDbAsyncClient: DynamoDbAsyncClient,
    private val tableNameProvider: TableNameProvider
) : ApplicationEventListener<StartupEvent> {

    companion object{
        private val log = LoggerFactory.getLogger(TableCreator::class.java)
    }

    override fun onApplicationEvent(event: StartupEvent) {
        if(!dynamoDbAsyncClient.listTables().get().tableNames().contains(tableNameProvider.tableName())){
            log.info("Setting up table: ${tableNameProvider.tableName()}")

            dynamoDbAsyncClient.createTable { createBuilder ->
                createBuilder
                    .attributeDefinitions(
                        AttributeDefinition.builder()
                            .attributeName("PK")
                            .attributeType(ScalarAttributeType.S)
                            .build(),
                        AttributeDefinition.builder()
                            .attributeName("SK")
                            .attributeType(ScalarAttributeType.S)
                            .build()
                    )
                    .keySchema(
                        KeySchemaElement.builder()
                            .attributeName("PK")
                            .keyType(KeyType.HASH)
                            .build(),
                        KeySchemaElement.builder()
                            .attributeName("SK")
                            .keyType(KeyType.RANGE)
                            .build()
                    )
                    .provisionedThroughput(
                        ProvisionedThroughput.builder()
                            .readCapacityUnits(10)
                            .writeCapacityUnits(10)
                            .build()
                    )
                    .tableName(tableNameProvider.tableName())
            }.get()
        }
    }
}
