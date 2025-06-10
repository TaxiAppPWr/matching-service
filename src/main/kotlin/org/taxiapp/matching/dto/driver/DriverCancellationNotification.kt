package org.taxiapp.matching.dto.driver

data class DriverCancellationNotification(
    val driverId: String,
    val rideId: Long,
    val eventType: String = "RIDE_CANCELLED"
)
