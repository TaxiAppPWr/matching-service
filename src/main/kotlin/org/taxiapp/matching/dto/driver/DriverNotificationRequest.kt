package org.taxiapp.matching.dto.driver

import java.math.BigDecimal

data class DriverNotificationRequest(
    val driverId: Long,
    val rideId: String,
    val pickupAddress: String,
    val dropoffAddress: String,
    val estimatedPrice: BigDecimal,
    val estimatedDistanceKm: Double,
    val timeoutMs: Long
)
