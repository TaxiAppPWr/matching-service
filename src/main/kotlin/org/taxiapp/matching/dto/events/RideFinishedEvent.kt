package org.taxiapp.matching.dto.events

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.time.OffsetDateTime

@JsonIgnoreProperties(ignoreUnknown = true)
data class RideFinishedEvent(
    val rideFinishedEventId: Long,
    val driverUsername: String,
    val rideId: Long,
    val startTime: OffsetDateTime,
    val endTime: OffsetDateTime,
    val driverEarning: Int
)