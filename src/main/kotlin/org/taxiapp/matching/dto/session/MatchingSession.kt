package org.taxiapp.matching.dto.session

import org.taxiapp.matching.dto.driver.NearbyDriver
import org.taxiapp.matching.dto.matching.MatchingRequest
import org.taxiapp.matching.dto.matching.MatchingResult
import org.taxiapp.matching.dto.matching.MatchingStatus
import java.time.LocalDateTime

data class MatchingSession(
    val request: MatchingRequest,
    val startedAt: LocalDateTime,
    var lastUpdateAt: LocalDateTime,
    var status: MatchingStatus,
    var availableDrivers: List<NearbyDriver> = emptyList(),
    var currentDriverId: String? = null,
    var attemptedDrivers: Int = 0,
    @Volatile var driverResponded: Boolean = false,
    @Volatile var lastDriverAccepted: Boolean = false,
    var result: MatchingResult? = null
)
