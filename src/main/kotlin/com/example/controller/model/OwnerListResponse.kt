package com.example.controller.model

import io.micronaut.core.annotation.Introspected

@Introspected
data class OwnerListResponse(
    val owners: List<Owner>
)
