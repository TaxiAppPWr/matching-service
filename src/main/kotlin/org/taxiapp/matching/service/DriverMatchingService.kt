package org.taxiapp.matching.service

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.taxiapp.matching.dto.driver.*
import org.taxiapp.matching.dto.matching.MatchingRequest
import org.taxiapp.matching.dto.matching.MatchingResponse
import org.taxiapp.matching.dto.matching.MatchingStatus
import org.taxiapp.matching.dto.session.MatchedDriver
import org.taxiapp.matching.dto.session.MatchingSession
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.cancellation.CancellationException
import org.taxiapp.matching.clients.MessagingServiceClient
import org.taxiapp.matching.clients.LocationServiceClient

@Service
class DriverMatchingService(
    private val driverServiceClient: LocationServiceClient,
    private val messagingServiceClient: MessagingServiceClient,
    @Value("\${matching.max-drivers-to-try}") private val maxDriversToTry: Int,
    @Value("\${matching.search-radius-km}") private val searchRadius: Int,
    @Value("\${matching.delay-between-attempts-ms}") private val delayBetweenAttempts: Long,
    @Value("\${services.messaging-service.driver-response-timeout}") private val driverResponseTimeout: Long
) {
    private val logger = LoggerFactory.getLogger(DriverMatchingService::class.java)
    private val activeMatchings = ConcurrentHashMap<String, MatchingSession>()

    suspend fun findDriver(request: MatchingRequest): MatchingResponse = coroutineScope {
        val matchingId = UUID.randomUUID().toString()
        val session = MatchingSession(
            matchingId = matchingId,
            request = request,
            startTime = System.currentTimeMillis()
        )

        activeMatchings[matchingId] = session

        try {
            logger.info("Starting driver matching for ride ${request.rideId} at location (${request.pickupLatitude}, ${request.pickupLongitude})")

            val nearbyDrivers = getNearbyDrivers(request)

            if (nearbyDrivers.isEmpty()) {
                logger.warn("No active drivers found for ride ${request.rideId}")
                return@coroutineScope createResponse(
                    session,
                    MatchingStatus.NO_DRIVERS_AVAILABLE,
                    "No available drivers in your area"
                )
            }

            logger.info("Found ${nearbyDrivers.size} nearby drivers for ride ${request.rideId}")

            val matchedDriver = tryMatchingWithDrivers(session, nearbyDrivers)

            return@coroutineScope if (matchedDriver != null) {
                createResponse(
                    session,
                    MatchingStatus.DRIVER_FOUND,
                    "Driver successfully matched",
                    matchedDriver
                )
            } else {
                createResponse(
                    session,
                    MatchingStatus.NO_DRIVERS_AVAILABLE,
                    "No drivers accepted the ride request"
                )
            }

        } catch (e: CancellationException) {
            logger.info("Matching cancelled for ride ${request.rideId}")
            return@coroutineScope createResponse(
                session,
                MatchingStatus.CANCELLED,
                "Matching process was cancelled"
            )
        } catch (e: Exception) {
            logger.error("Error during matching for ride ${request.rideId}", e)
            return@coroutineScope createResponse(
                session,
                MatchingStatus.NO_DRIVERS_AVAILABLE,
                "Error occurred during matching: ${e.message}"
            )
        } finally {
            activeMatchings.remove(matchingId)
        }
    }

    suspend fun cancelMatching(matchingId: String): Boolean {
        val session = activeMatchings[matchingId]
        return if (session != null) {
            session.cancelled = true
            activeMatchings.remove(matchingId)
            logger.info("Cancelled matching session $matchingId")
            true
        } else {
            false
        }
    }

    fun getMatchingStatus(matchingId: String): MatchingSession? {
        return activeMatchings[matchingId]
    }

    private suspend fun getNearbyDrivers(request: MatchingRequest): List<NearbyDriver> {
        val nearbyRequest = NearbyDriversRequest(
            latitude = request.pickupLatitude,
            longitude = request.pickupLongitude,
            radius = searchRadius,
            limit = maxDriversToTry
        )

        return driverServiceClient.getNearbyDrivers(nearbyRequest)
            .filter { it.isActive }
            .sortedBy { it.distance }
            .take(maxDriversToTry)
    }

    private suspend fun tryMatchingWithDrivers(
        session: MatchingSession,
        drivers: List<NearbyDriver>
    ): MatchedDriver? {
        for ((index, driver) in drivers.withIndex()) {
            if (session.cancelled) {
                logger.info("Matching cancelled during driver attempts for ride ${session.request.rideId}")
                break
            }

            logger.info("Attempting to match with driver ${driver.id} (${index + 1}/${drivers.size}) for ride ${session.request.rideId}")

            val attemptStartTime = System.currentTimeMillis()
            val notificationRequest = DriverNotificationRequest(
                driverId = driver.id,
                rideId = session.request.rideId,
                pickupAddress = session.request.pickupAddress,
                dropoffAddress = session.request.dropoffAddress,
                estimatedPrice = session.request.estimatedPrice,
                estimatedDistanceKm = driver.distance,
                timeoutMs = driverResponseTimeout
            )

            try {
                val response = messagingServiceClient.notifyDriver(notificationRequest)
                val responseTime = System.currentTimeMillis() - attemptStartTime

                val attemptInfo = DriverAttemptInfo(
                    driverId = driver.id,
                    distance = driver.distance,
                    response = if (response.accepted) DriverResponseType.ACCEPTED else DriverResponseType.DECLINED,
                    responseTimeMs = responseTime
                )
                session.attempts.add(attemptInfo)

                if (response.accepted) {
                    logger.info("Driver ${driver.id} accepted ride ${session.request.rideId}")
                    return MatchedDriver(
                        driverId = driver.id,
                        driverName = "Driver ${driver.id}",
                        estimatedArrivalMinutes = calculateEstimatedArrival(driver.distance)
                    )
                } else {
                    logger.info("Driver ${driver.id} declined ride ${session.request.rideId}: ${response.reason}")
                }

            } catch (e: Exception) {
                logger.error("Error notifying driver ${driver.id} for ride ${session.request.rideId}", e)

                val attemptInfo = DriverAttemptInfo(
                    driverId = driver.id,
                    distance = driver.distance,
                    response = DriverResponseType.TIMEOUT,
                    responseTimeMs = driverResponseTimeout
                )
                session.attempts.add(attemptInfo)
            }

            if (index < drivers.size - 1 && !session.cancelled) {
                delay(delayBetweenAttempts)
            }
        }

        return null
    }

    private fun calculateEstimatedArrival(distanceKm: Double): Int {
        // Simple calculation - assume average speed of 30 km/h in city
        val estimatedMinutes = (distanceKm / 30.0 * 60).toInt()
        return estimatedMinutes.coerceAtLeast(2)
    }

    private fun createResponse(
        session: MatchingSession,
        status: MatchingStatus,
        message: String,
        matchedDriver: MatchedDriver? = null
    ): MatchingResponse {
        return MatchingResponse(
            rideId = session.request.rideId,
            matchingId = session.matchingId,
            status = status,
            driverId = matchedDriver?.driverId,
            driverName = matchedDriver?.driverName,
            estimatedArrivalMinutes = matchedDriver?.estimatedArrivalMinutes,
            attemptedDrivers = session.attempts,
            message = message
        )
    }
}