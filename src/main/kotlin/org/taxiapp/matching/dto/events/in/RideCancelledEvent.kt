package org.taxiapp.matching.dto.events.`in`

data class RideCancelledEvent(
    val cancelRideEventId: Long,
    val rideId: Long,
    val refundPercentage: Int,
    val driverId: String,
)

