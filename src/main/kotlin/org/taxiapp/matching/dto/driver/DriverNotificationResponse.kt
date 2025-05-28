package org.taxiapp.matching.dto.driver

data class DriverNotificationResponse(
    val notificationId: String,
    val driverId: Long,
    val accepted: Boolean,
    val responseTimeMs: Long,
    val reason: String? = null
)
