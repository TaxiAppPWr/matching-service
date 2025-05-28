package org.taxiapp.matching.dto.driver

import java.time.LocalDateTime

data class NearbyDriver(
    val id: Long,
    val distance: Double,
    val isActive: Boolean,
    val lastPing: LocalDateTime,
    val longitude: Double,
    val latitude: Double
)