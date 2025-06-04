package org.taxiapp.matching.dto.events.`in`

data class RideCancelledEvent(
    val rideId: Long,
    val driverId: String
)

