package org.taxiapp.matching.dto.driver

import java.time.LocalDateTime

data class NearbyDriver(
    val driverId: String,
    val distance: Double,
    val isActive: Boolean,
    val lastPing: LocalDateTime,
    val longitude: Double,
    val latitude: Double
)