package com.example.testdata

import com.example.repository.Owner

object OwnerTestData {
    val sampleOwner = Owner(
        id = "owner_id_1",
        address = Owner.OwnerAddress(
            street = "Street1",
            country = "EU",
            postalCode = "12345"
        ),
        tel = "+49 123 2345 001",
        name = "Arthur Dent"
    )
}
