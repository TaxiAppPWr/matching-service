package org.taxiapp.matching.dto.matching

import java.time.LocalDateTime

data class MatchingResult(
    val driverId: String,
    val acceptedAt: LocalDateTime
)