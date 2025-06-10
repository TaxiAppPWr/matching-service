package org.taxiapp.matching.dto.driver

import java.math.BigDecimal

data class DriverRideOfferNotification(
    val driverId: String,
    val rideId: Long,
    val pickupAddress: String,
    val pickupLatitude: Double,
    val pickupLongitude: Double,
    val dropoffAddress: String,
    val dropoffLatitude: Double,
    val dropoffLongitude: Double,
    val estimatedPrice: BigDecimal,
    val eventType: String = "RIDE_OFFER"
)
