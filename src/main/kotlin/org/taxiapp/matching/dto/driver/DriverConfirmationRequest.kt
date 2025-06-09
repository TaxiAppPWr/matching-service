package org.taxiapp.matching.dto.driver

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.PositiveOrZero

data class DriverConfirmation(
    @field:NotNull @field:PositiveOrZero
    val rideId: Long,

    @field:NotNull @field:NotBlank
    val driverId: String,

    @field:NotNull
    val accepted: Boolean,

)

data class DriverConfirmationRequest(
    @field:NotNull @field:PositiveOrZero
    val rideId: Long,

    @field:NotNull
    val accepted: Boolean,

    )

