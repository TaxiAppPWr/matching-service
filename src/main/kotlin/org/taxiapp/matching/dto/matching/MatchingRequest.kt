package org.taxiapp.matching.dto.matching

import jakarta.validation.constraints.*
import java.math.BigDecimal

// Request from Ride Service
data class MatchingRequest(
    @field:NotBlank
    val rideId: String,

    @field:NotNull @field:DecimalMin("-90.0") @field:DecimalMax("90.0")
    val pickupLatitude: Double,

    @field:NotNull @field:DecimalMin("-180.0") @field:DecimalMax("180.0")
    val pickupLongitude: Double,

    @field:NotBlank
    val pickupAddress: String,

    @field:NotBlank
    val dropoffAddress: String,

    @field:NotNull @field:Positive
    val estimatedPrice: BigDecimal,

    @field:NotNull @field:Positive
    val passengerId: Long,

    val maxWaitTimeSeconds: Int = 300 // Maximum time to find a driver
)
