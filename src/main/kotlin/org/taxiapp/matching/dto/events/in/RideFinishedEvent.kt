package org.taxiapp.matching.dto.events.`in`

import java.time.OffsetDateTime

data class RideFinishedEvent(
    val rideFinishedEventId: Long,
    val driverUsername: String,
    val rideId: Long,
    val startTime: OffsetDateTime,
    val endTime: OffsetDateTime,
    val driverEarning: Int
)
