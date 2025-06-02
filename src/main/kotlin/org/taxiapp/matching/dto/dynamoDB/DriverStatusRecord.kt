package org.taxiapp.matching.dto.dynamoDB

enum class DriverStatus {
    PENDING_REQUEST,
    RIDING_CURRENTLY;

    companion object {
        fun mapToDriverStatus(status: String): DriverStatus {
            return when (status) {
                "RIDING_CURRENTLY" -> RIDING_CURRENTLY
                else -> PENDING_REQUEST
            }
        }
    }
}

data class DriverStatusRecord(
    var driverId: String,
    var status: DriverStatus
)
