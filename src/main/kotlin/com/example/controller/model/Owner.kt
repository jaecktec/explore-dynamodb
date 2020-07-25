package com.example.controller.model

import com.fasterxml.jackson.annotation.JsonCreator
import io.micronaut.core.annotation.Introspected
import javax.validation.Valid
import javax.validation.constraints.AssertTrue
import javax.validation.constraints.NotBlank

@Introspected
data class Owner(
    val id: String = "",
    @field:NotBlank
    val name: String = "",
    @field:NotBlank
    val tel: String = "",
    @field:Valid
    val address: Address = Address(
        street = "",
        postalCode = "",
        country = ""
    )
) {
    @AssertTrue(message = "id must be absent", groups = [CreateOwnerValidationGroup::class])
    fun isAssertTrue(): Boolean = id.isBlank()

    @JsonCreator
    constructor(): this("","","", Address())
}

interface CreateOwnerValidationGroup
