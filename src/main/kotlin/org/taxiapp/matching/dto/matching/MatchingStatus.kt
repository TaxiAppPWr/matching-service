package org.taxiapp.matching.dto.matching

enum class MatchingStatus {
    IN_PROGRESS,
    DRIVER_FOUND,
    NO_DRIVERS_AVAILABLE,
    CANCELLED,
    TIMEOUT
}