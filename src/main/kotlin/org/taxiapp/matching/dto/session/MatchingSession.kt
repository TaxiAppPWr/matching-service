package org.taxiapp.matching.dto.session

import org.taxiapp.matching.dto.driver.DriverAttemptInfo
import org.taxiapp.matching.dto.matching.MatchingRequest

data class MatchingSession(
    val matchingId: String,
    val request: MatchingRequest,
    val startTime: Long,
    val attempts: MutableList<DriverAttemptInfo> = mutableListOf(),
    @Volatile var cancelled: Boolean = false
)

