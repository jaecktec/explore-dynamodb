package com.example.controller

import com.example.controller.model.Address
import com.example.controller.model.NewResourceResponse
import com.example.controller.model.Owner
import com.example.controller.model.OwnerListResponse
import com.example.repository.Owner.OwnerAddress
import com.example.repository.OwnerRepository
import com.example.repository.OwnerRepositoryTest
import com.example.testdata.OwnerTestData
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.TextNode
import com.github.fge.jackson.jsonpointer.JsonPointer
import com.github.fge.jsonpatch.JsonPatch
import com.github.fge.jsonpatch.JsonPatchOperation
import com.github.fge.jsonpatch.PathValueOperation
import com.github.fge.jsonpatch.ReplaceOperation
import io.micronaut.http.HttpHeaders
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpStatus
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.http.client.exceptions.HttpClientResponseException
import io.micronaut.http.hateoas.JsonError
import io.micronaut.test.annotation.MicronautTest
import io.micronaut.test.annotation.MockBean
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import javax.inject.Inject

@MicronautTest
class OwnerControllerTest {

    companion object {
        private val sampleRequestAddr = Address(
            street = OwnerTestData.sampleOwner.address.street,
            postalCode = OwnerTestData.sampleOwner.address.postalCode,
            country = OwnerTestData.sampleOwner.address.country
        )
        private val sampleRequestOwner = Owner(
            name = OwnerTestData.sampleOwner.name,
            tel = OwnerTestData.sampleOwner.tel,
            address = sampleRequestAddr
        )
    }

    @Inject
    @field:Client("/owner")
    private lateinit var client: HttpClient

    @Inject
    private lateinit var ownerRepository: OwnerRepository

    @MockBean(OwnerRepository::class)
    fun mockOwnerRepository() = mockk<OwnerRepository>()

    @Test
    fun `get - should return owner by id`() {
        coEvery { ownerRepository.getById("owner_id") } returns OwnerTestData.sampleOwner

        val retrieve = client.toBlocking().exchange("/owner_id", Owner::class.java)

        assertEquals(HttpStatus.OK, retrieve.status)
        val body = retrieve.getBody(Owner::class.java).get()
        assertEquals(OwnerTestData.sampleOwner.id, body.id)
        assertEquals(OwnerTestData.sampleOwner.name, body.name)
        assertEquals(OwnerTestData.sampleOwner.tel, body.tel)
        assertEquals(OwnerTestData.sampleOwner.address.country, body.address.country)
        assertEquals(OwnerTestData.sampleOwner.address.postalCode, body.address.postalCode)
        assertEquals(OwnerTestData.sampleOwner.address.street, body.address.street)
    }

    @Test
    fun `get - when owner does not exist - should return 404`() {
        coEvery { ownerRepository.getById("owner_id") } returns null

        val errorResponse = assertThrows<HttpClientResponseException> {
            client.toBlocking().exchange("/owner_id", Owner::class.java)
        }

        assertEquals(HttpStatus.NOT_FOUND, errorResponse.status)
        assertEquals("owner_not_found", errorResponse.response.getBody(JsonError::class.java).get().message)
    }

    @Test
    fun `list - returns all owners`() {
        coEvery { ownerRepository.getAll() } returns listOf(OwnerTestData.sampleOwner)

        val retrieve = client.toBlocking().exchange("/", OwnerListResponse::class.java)

        assertEquals(HttpStatus.OK, retrieve.status)
        val body = retrieve.getBody(OwnerListResponse::class.java).get()
        assertEquals(1, body.owners.size)
        assertEquals(OwnerTestData.sampleOwner.id, body.owners.first().id)
    }

    @Test
    fun `save - stores new owner`() {
        coEvery { ownerRepository.save(any()) } returns "new_owner_id"

        val retrieve = client.toBlocking().exchange(
            HttpRequest.POST(
                "", sampleRequestOwner
            ),
            NewResourceResponse::class.java
        )

        assertEquals(HttpStatus.CREATED, retrieve.status)
        assertEquals("/owner/new_owner_id", retrieve.header(HttpHeaders.LOCATION))
        assertEquals("/owner/new_owner_id", retrieve.getBody(NewResourceResponse::class.java).get().resource)

        coVerify { ownerRepository.save(OwnerTestData.sampleOwner.copy(id = "")) }
    }

    @Test
    fun `update - updates owner`() {
        coEvery { ownerRepository.getById("owner_id") } returns OwnerTestData.sampleOwner.copy(id = "owner_id")
        coEvery { ownerRepository.save(any()) } returns "owner_id"

        val retrieve = client.toBlocking().exchange(
            HttpRequest.PUT(
                "/owner_id", JsonPatch(listOf(
                    ReplaceOperation(JsonPointer("/name"), TextNode("new_name")),
                    ReplaceOperation(JsonPointer("/tel"), TextNode("new_tel")),
                    ReplaceOperation(JsonPointer("/address/street"), TextNode("new_street")),
                    ReplaceOperation(JsonPointer("/address/country"), TextNode("new_country")),
                    ReplaceOperation(JsonPointer("/address/postalCode"), TextNode("new_postal"))
                ))
            ),
            Void::class.java
        )

        assertEquals(HttpStatus.NO_CONTENT, retrieve.status)

        coVerify { ownerRepository.save(OwnerTestData.sampleOwner.copy(
            id = "owner_id",
            name = "new_name",
            tel = "new_tel",
            address = OwnerAddress(
                street = "new_street",
                country = "new_country",
                postalCode = "new_postal"
            )
        )) }
    }

    enum class SaveParamsBadRequest(
        val owner: Owner, val expectedErrorMessage: String
    ) {
        NO_NAME(owner = sampleRequestOwner.copy(name = ""), expectedErrorMessage = "name: must not be blank"),
        NO_TEL(owner = sampleRequestOwner.copy(tel = ""), expectedErrorMessage = "tel: must not be blank"),
        NO_ADDRESS_STREET(owner = sampleRequestOwner.copy(address = sampleRequestAddr.copy(street = "")), expectedErrorMessage = "address.street: must not be blank"),
        NO_ADDRESS_PLZ(owner = sampleRequestOwner.copy(address = sampleRequestAddr.copy(postalCode = "")), expectedErrorMessage = "address.postalCode: must not be blank"),
        NO_ADDRESS_COUNTRY(owner = sampleRequestOwner.copy(address = sampleRequestAddr.copy(country = "")), expectedErrorMessage = "address.country: must not be blank"),
        ID_PRESENT(owner = sampleRequestOwner.copy(id = "some_owner_id"), expectedErrorMessage = "assertTrue: id must be absent")
    }

    @ParameterizedTest
    @EnumSource(value = SaveParamsBadRequest::class)
    fun `save - when parameter missing - returns bad request`(arg: SaveParamsBadRequest) {
        coEvery { ownerRepository.save(any()) } returns ""

        val errorResponse = assertThrows<HttpClientResponseException> {
            client.toBlocking().exchange(
                HttpRequest.POST(
                    "", arg.owner
                ),
                NewResourceResponse::class.java
            )
        }

        assertEquals(HttpStatus.BAD_REQUEST, errorResponse.status)
        assertEquals(arg.expectedErrorMessage, errorResponse.response.getBody(JsonError::class.java).get().message)

        coVerify(exactly = 0) { ownerRepository.save(any()) }
    }

}
