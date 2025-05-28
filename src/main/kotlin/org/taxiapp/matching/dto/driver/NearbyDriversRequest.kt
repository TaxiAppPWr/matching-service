package org.taxiapp.matching.dto.driver

data class NearbyDriversRequest(
    val longitude: Double,
    val latitude: Double,
    val radius: Int,
    val limit: Int
)
