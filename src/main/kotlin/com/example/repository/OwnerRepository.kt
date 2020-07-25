package com.example.repository

import com.example.factory.TableNameProvider
import kotlinx.coroutines.future.await
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient
import software.amazon.awssdk.services.dynamodb.model.AttributeValue
import software.amazon.awssdk.services.dynamodb.model.ReturnValue
import java.util.UUID
import javax.inject.Singleton

@Singleton
class OwnerRepository(
    private val dynamoDbClient: DynamoDbAsyncClient,
    private val tableNameProvider: TableNameProvider
) {

    object Fields {
        const val ID = "OWNER_ID"
        const val NAME = "OWNER_NAME"
        const val TEL = "OWNER_TEL"
        const val ADDRESS = "OWNER_ADDRESS"
    }

    suspend fun getAll(): List<Owner> {
        val queryResponse = dynamoDbClient.query {
            it.tableName(tableNameProvider.tableName())
                .keyConditionExpression("PK = :collection")
                .expressionAttributeValues(
                    mapOf(
                        ":collection" to AttributeValue.builder().s("OWNER").build()
                    )
                )
        }.await()

        return queryResponse.items().map { it.toOwner() }
    }

    suspend fun getById(id: String): Owner? {
        val await = dynamoDbClient.getItem {
            it.tableName(tableNameProvider.tableName())
                .key(
                    mapOf(
                        "PK" to AttributeValue.builder().s("OWNER").build(),
                        "SK" to AttributeValue.builder().s("OWNER:$id").build()
                    )
                )
        }.await()

        return await.takeIf { it.hasItem() }?.item()?.toOwner()
    }

    suspend fun save(owner: Owner): String {
        return if (owner.id.isNotBlank()) {
            dynamoDbClient.updateItem {
                 it.tableName(tableNameProvider.tableName())
                    .key(owner.toKeyMap())
                    .updateExpression("SET ${Fields.NAME} = :name, ${Fields.TEL} = :tel, ${Fields.ADDRESS} = :address")
                    .expressionAttributeValues(owner.toAttributeMap().mapKeys { k -> ":${k.key.removePrefix("OWNER_").toLowerCase()}" })
                    .returnValues(ReturnValue.ALL_NEW)
            }.await()
            owner.id
        } else {
            val ownerToSave = owner.copy(id = UUID.randomUUID().toString())
            dynamoDbClient.putItem {
                it.tableName(tableNameProvider.tableName())
                    .item(ownerToSave.toAttributeMap() + ownerToSave.toKeyMap() + ownerToSave.toImmutableAttributeMap())
            }.await()
            ownerToSave.id
        }
    }

    private fun Owner.toKeyMap(): Map<String, AttributeValue> {
        return mapOf(
            "PK" to AttributeValue.builder().s("OWNER").build(),
            "SK" to AttributeValue.builder().s("OWNER:$id").build()
        )
    }
    private fun Owner.toImmutableAttributeMap(): Map<String, AttributeValue> {
        return mapOf(
            Fields.ID to AttributeValue.builder().s(id).build()
        )
    }
    private fun Owner.toAttributeMap(): Map<String, AttributeValue> {
        return mapOf(
            Fields.ADDRESS to AttributeValue.builder().m(
                mapOf(
                    "STREET" to AttributeValue.builder().s(address.street).build(),
                    "POSTAL_CODE" to AttributeValue.builder().s(address.postalCode).build(),
                    "COUNTRY" to AttributeValue.builder().s(address.country).build()
                )
            ).build(),
            Fields.NAME to AttributeValue.builder().s(name).build(),
            Fields.TEL to AttributeValue.builder().s(tel).build()
        )
    }

    private fun MutableMap<String, AttributeValue>.toOwner(): Owner {
        return Owner(
            id = this[Fields.ID]!!.s(),
            name = this[Fields.NAME]!!.s(),
            tel = this[Fields.TEL]!!.s(),
            address = this[Fields.ADDRESS]!!.m().let { m ->
                Owner.OwnerAddress(
                    street = m["STREET"]!!.s(),
                    postalCode = m["POSTAL_CODE"]!!.s(),
                    country = m["COUNTRY"]!!.s()
                )
            }
        )
    }
}

data class Owner(var id: String = "", var name: String, var tel: String, var address: OwnerAddress) {
    data class OwnerAddress(val street: String, val postalCode: String, val country: String)
}
