package org.taxiapp.matching.dto.events.`in`

data class RideFinishedEvent(
    val rideId: Long,
    val driverId: String,
)
