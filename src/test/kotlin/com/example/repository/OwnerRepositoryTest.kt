package com.example.repository

import com.example.factory.TableNameProvider
import com.example.testdata.OwnerTestData
import io.micronaut.test.annotation.MicronautTest
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient
import software.amazon.awssdk.services.dynamodb.model.AttributeValue
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest
import javax.inject.Inject

@MicronautTest
class OwnerRepositoryTest {

    @Inject
    private lateinit var dynamoDbClient: DynamoDbAsyncClient

    @Inject
    private lateinit var tableName: TableNameProvider

    @Inject
    private lateinit var subject: OwnerRepository

    @Test
    fun `get all - should return all owners`() {
        storeSampleOwner()

        val actual = runBlocking {
            subject.getAll()
        }


        assertEquals(
            listOf(
                OwnerTestData.sampleOwner
            ), actual
        )
    }

    @Test
    fun `get one - should return element by id`() {
        storeSampleOwner()

        val actual = runBlocking {
            subject.getById("owner_id_1")
        }

        assertNotNull(actual)
        assertEquals(actual, OwnerTestData.sampleOwner)
    }

    @Test
    fun `get one - when not found should return null`() {
        val actual = runBlocking {
            subject.getById("owner_id_1")
        }

        assertNull(actual)
    }

    @Test
    fun `save - should persist new owner`() {
         val actual = runBlocking {
            val id = subject.save(OwnerTestData.sampleOwner.copy(id = ""))

            subject.getById(id)
        }

        assertNotNull(actual)
        assertTrue(actual!!.id.isNotBlank())
        assertEquals(actual, OwnerTestData.sampleOwner.copy(id = actual.id))
    }

    @Test
    fun `save - should update existing owner`() {
        val (_, name, tel, address) = runBlocking {
            val id = subject.save(OwnerTestData.sampleOwner.copy(id = ""))
            subject.save(
                OwnerTestData.sampleOwner.copy(
                    id = id,
                    name = "updatedName",
                    tel = "updatedPhone",
                    address = Owner.OwnerAddress(
                        street = "updateStreet",
                        postalCode = "updatedPostalCode",
                        country = "updateCountry"
                    )
                )
            )
            subject.getById(id)!!
        }

        assertEquals("updatedName", name)
        assertEquals("updatedPhone", tel)
        assertEquals(Owner.OwnerAddress(
            street = "updateStreet",
            postalCode = "updatedPostalCode",
            country = "updateCountry"
        ), address)
    }

    @AfterEach
    fun tearDown() {
        dynamoDbClient.scan {
            it.tableName(tableName.tableName())
        }.get().items().forEach {
            dynamoDbClient.deleteItem(
                DeleteItemRequest.builder()
                    .tableName(tableName.tableName())
                    .key(
                        mapOf(
                            "PK" to it["PK"],
                            "SK" to it["SK"]
                        )
                    ).build()
            ).get()
        }
    }

    private fun storeSampleOwner() {
        dynamoDbClient.putItem {
            it
                .tableName(tableName.tableName())
                .item(
                    mapOf(
                        "PK" to AttributeValue.builder().s("OWNER").build(),
                        "SK" to AttributeValue.builder().s("OWNER:owner_id_1").build(),
                        OwnerRepository.Fields.ID to AttributeValue.builder().s("owner_id_1").build(),
                        OwnerRepository.Fields.ADDRESS to AttributeValue.builder().m(
                            mapOf(
                                "STREET" to AttributeValue.builder().s("Street1").build(),
                                "POSTAL_CODE" to AttributeValue.builder().s("12345").build(),
                                "COUNTRY" to AttributeValue.builder().s("EU").build()
                            )
                        ).build(),
                        OwnerRepository.Fields.NAME to AttributeValue.builder().s("Arthur Dent").build(),
                        OwnerRepository.Fields.TEL to AttributeValue.builder().s("+49 123 2345 001").build()
                    )
                )
        }.get()
    }
}
