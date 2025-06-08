package org.taxiapp.matching.dto.matching

import jakarta.validation.constraints.*
import java.math.BigDecimal

data class MatchingRequest(
    @field:NotNull @field:PositiveOrZero
    val rideId: Long,

    @field:NotNull
    val pickupAddress: String,

    @field:NotNull @field:DecimalMin("-90.0") @field:DecimalMax("90.0")
    val pickupLatitude: Double,

    @field:NotNull @field:DecimalMin("-180.0") @field:DecimalMax("180.0")
    val pickupLongitude: Double,

    @field:NotNull
    val dropoffAddress: String,

    @field:NotNull @field:DecimalMin("-90.0") @field:DecimalMax("90.0")
    val dropoffLatitude: Double,

    @field:NotNull @field:DecimalMin("-180.0") @field:DecimalMax("180.0")
    val dropoffLongitude: Double,

    @field:NotNull @field:Positive
    val estimatedPrice: BigDecimal,
)
