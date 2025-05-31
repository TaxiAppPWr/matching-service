package org.taxiapp.matching.dto.matching

enum class MatchingStatus {
    IN_PROGRESS,
    WAITING_CONFIRMATION,
    COMPLETED,
    NO_DRIVERS_AVAILABLE,
    CANCELLED,
    FAILED
}