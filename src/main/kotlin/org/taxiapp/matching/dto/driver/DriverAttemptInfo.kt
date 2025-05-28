package org.taxiapp.matching.dto.driver

data class DriverAttemptInfo(
    val driverId: Long,
    val distance: Double,
    val response: DriverResponseType,
    val responseTimeMs: Long?
)
