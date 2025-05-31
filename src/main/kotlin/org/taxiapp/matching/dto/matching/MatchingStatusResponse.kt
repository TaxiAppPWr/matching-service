package org.taxiapp.matching.dto.matching

import java.time.LocalDateTime

data class MatchingStatusResponse(
    val rideId: Long,
    val status: MatchingStatus,
    val currentDriverId: String? = null,
    val attemptedDrivers: Int = 0,
    val startedAt: LocalDateTime,
    val lastUpdateAt: LocalDateTime,
    val result: MatchingResult? = null
)