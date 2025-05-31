package org.taxiapp.matching.dto.driver

import java.math.BigDecimal

data class DriverNotificationRequest(
    val driverId: String,
    val rideId: Long,
    val pickupLatitude: Double,
    val pickupLongitude: Double,
    val dropoffLatitude: Double,
    val dropoffLongitude: Double,
    val estimatedPrice: BigDecimal,
)
