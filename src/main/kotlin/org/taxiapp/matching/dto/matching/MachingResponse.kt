package org.taxiapp.matching.dto.matching

import org.taxiapp.matching.dto.driver.DriverAttemptInfo

data class MatchingResponse(
    val rideId: String,
    val matchingId: String,
    val status: MatchingStatus,
    val driverId: Long? = null,
    val driverName: String? = null,
    val driverPhone: String? = null,
    val estimatedArrivalMinutes: Int? = null,
    val attemptedDrivers: List<DriverAttemptInfo> = emptyList(),
    val message: String
)
