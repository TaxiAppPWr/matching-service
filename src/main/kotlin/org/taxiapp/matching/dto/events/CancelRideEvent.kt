package org.taxiapp.matching.dto.events

data class CancelRideEvent(
    val cancelRideEventId: Long,
    val rideId: Long,
    val refundPercentage: Int
)