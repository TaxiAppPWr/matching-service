package org.taxiapp.matching.dto.session

data class MatchedDriver(
    val driverId: Long,
    val driverName: String,
    val estimatedArrivalMinutes: Int
)

