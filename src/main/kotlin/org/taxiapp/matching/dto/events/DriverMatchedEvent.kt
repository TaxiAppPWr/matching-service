package org.taxiapp.matching.dto.events

import java.time.LocalDateTime

data class DriverMatchedEvent(
    val rideId: Long,
    val driverId: String,
    val matchedAt: LocalDateTime,
)
