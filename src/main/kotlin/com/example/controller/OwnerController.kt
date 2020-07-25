package com.example.controller

import com.example.controller.model.Address
import com.example.controller.model.CreateOwnerValidationGroup
import com.example.controller.model.NewResourceResponse
import com.example.controller.model.Owner
import com.example.controller.model.OwnerListResponse
import com.example.repository.OwnerRepository
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.github.fge.jsonpatch.JsonPatch
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Error
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.PathVariable
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.Put
import io.micronaut.http.annotation.Status
import io.micronaut.http.hateoas.JsonError
import java.net.URI
import javax.validation.ConstraintViolationException
import javax.validation.ValidationException
import javax.validation.ValidatorFactory
import javax.validation.groups.Default
import com.example.repository.Owner as RepositoryOwner

@Controller("/owner")
class OwnerController(
    private val ownerRepository: OwnerRepository,
    private val validatorFactory: ValidatorFactory,
    private val objectMapper: ObjectMapper
) {

    @Get("/{ownerId}")
    suspend fun getOwnerById(@PathVariable(name = "ownerId") ownerId: String): Owner {
        val byId = ownerRepository.getById(ownerId) ?: throw OwnerNotFoundException(ownerId)
        return byId.toOwner()
    }

    @Get("/")
    suspend fun listOwners(): OwnerListResponse {
        return OwnerListResponse(ownerRepository.getAll().map { it.toOwner() })
    }

    @Post("/")
    suspend fun createOwner(@Body owner: Owner) : HttpResponse<NewResourceResponse> {
        val validator = validatorFactory.validator
        val validate = validator.validate(owner, CreateOwnerValidationGroup::class.java, Default::class.java)
        validate.takeIf { it.size > 0 }?.run { throw ConstraintViolationException(
            "create_owner_validation_error",
            this
        ) }
        val ownerId = ownerRepository.save(
            RepositoryOwner(
                name = owner.name,
                tel = owner.tel,
                address = RepositoryOwner.OwnerAddress(
                    street = owner.address.street,
                    country = owner.address.country,
                    postalCode = owner.address.postalCode
                )
            )
        )
        return HttpResponse.created(NewResourceResponse(resource = "/owner/$ownerId"), URI.create("/owner/$ownerId"))
    }

    @Put("/{ownerId}")
    @Status(HttpStatus.NO_CONTENT)
    suspend fun updateOwner(@PathVariable(name = "ownerId") ownerId: String, @Body jsonPatch: JsonPatch){
        val byId = ownerRepository.getById(ownerId)
        val owner = objectMapper.convertValue(jsonPatch.apply(objectMapper.convertValue(byId, JsonNode::class.java)), Owner::class.java)
        // TODO write tests for the validation? Need another opinion on that
        validatorFactory.validator.validate(owner).takeIf { it.size > 0 }?.run { throw ConstraintViolationException(
            "update_owner_validation_error",
            this
        ) }
        ownerRepository.save(RepositoryOwner(
            id = ownerId,
            name = owner.name,
            tel = owner.tel,
            address = RepositoryOwner.OwnerAddress(
                street = owner.address.street,
                country = owner.address.country,
                postalCode = owner.address.postalCode
            )
        ))
    }

    @Error(exception = OwnerNotFoundException::class)
    fun ownerNotFoundException(request: HttpRequest<*>, ex: OwnerNotFoundException): HttpResponse<JsonError> {
        return HttpResponse.notFound(
            JsonError("owner_not_found").path(request.uri.toString())
        )
    }

    data class OwnerNotFoundException(val ownerId: String) : RuntimeException()

    private fun RepositoryOwner.toOwner(): Owner {
        return Owner(
            id = id,
            name = name,
            tel = tel,
            address = Address(
                street = address.street,
                postalCode = address.postalCode,
                country = address.country
            )
        )
    }
}

