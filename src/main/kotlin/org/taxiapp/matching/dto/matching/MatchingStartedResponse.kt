package org.taxiapp.matching.dto.matching

data class MatchingStartedResponse(
    val rideId: Long,
    val status: MatchingStatus = MatchingStatus.IN_PROGRESS,
    val message: String = "Driver matching process started"
)
