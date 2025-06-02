package org.taxiapp.matching.dto.events.out

import java.time.LocalDateTime

data class MatchingFailedEvent(
    val rideId: Long,
    val reason: String,
    val failedAt: LocalDateTime
)