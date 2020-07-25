package com.example.controller.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import io.micronaut.core.annotation.Introspected
import javax.inject.Inject
import javax.validation.constraints.NotBlank

@Introspected
data class Address(
    @field:NotBlank
    val street: String = "",

    @field:NotBlank
    val postalCode: String = "",

    @field:NotBlank
    val country: String = ""
){
    @JsonCreator
    constructor(): this("","","")
}
