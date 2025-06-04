package org.taxiapp.matching.service

import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.taxiapp.matching.dto.driver.*
import org.taxiapp.matching.dto.session.MatchingSession
import java.util.concurrent.ConcurrentHashMap
import org.taxiapp.matching.client.NotificationServiceClient
import org.taxiapp.matching.client.LocationServiceClient
import org.taxiapp.matching.dto.dynamoDB.DriverStatus
import org.taxiapp.matching.dto.events.out.DriverMatchedEvent
import org.taxiapp.matching.dto.events.out.MatchingFailedEvent
import org.taxiapp.matching.dto.matching.*
import org.taxiapp.matching.repository.DriverRepository
import java.time.LocalDateTime

@Service
class DriverMatchingService(
    private val locationServiceClient: LocationServiceClient,
    private val messagingServiceClient: NotificationServiceClient,
    private val driverRepository: DriverRepository,
    private val rabbitTemplate: RabbitTemplate,
    @Value("\${matching.max-drivers-to-try}") private val maxDriversToTry: Int,
    @Value("\${matching.search-radius-km}") private val searchRadius: Int,
    @Value("\${matching.driver-confirmation-timeout-seconds}") private val confirmationTimeout: Long,
    @Value("\${matching.delay-between-attempts-ms}") private val delayBetweenAttempts: Long,
    @Value("\${rabbitmq.exchange.driver-matching}") private val exchangeName: String,
    @Value("\${rabbitmq.routing-key.driver-matching}") private val routingKey: String
) {
    private val logger = LoggerFactory.getLogger(DriverMatchingService::class.java)
    private val activeMatchings = ConcurrentHashMap<Long, MatchingSession>()
    private val matchingScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    fun startMatching(request: MatchingRequest): MatchingStartedResponse {
        val rideId = request.rideId
        val session = MatchingSession(
            request = request,
            startedAt = LocalDateTime.now(),
            lastUpdateAt = LocalDateTime.now(),
            status = MatchingStatus.IN_PROGRESS
        )

        activeMatchings[rideId] = session

        // Start the matching process asynchronously
        matchingScope.launch {
            try {
                performMatching(session)
            } catch (e: Exception) {
                logger.error("Error in matching process for ride $request.rideId", e)
                session.status = MatchingStatus.FAILED
                publishMatchingFailed(session, "Internal error: ${e.message}")
            }
        }

        logger.info("Started matching process for ride ${request.rideId}")
        return MatchingStartedResponse(rideId = rideId)
    }

    suspend fun confirmDriver(confirmation: DriverConfirmationRequest): Boolean {
        val session = activeMatchings[confirmation.rideId] ?: return false

        // Check if we're waiting for this specific driver
        if (session.currentDriverId != confirmation.driverId ||
            session.status != MatchingStatus.WAITING_CONFIRMATION) {
            logger.warn("Invalid confirmation: rideId=${confirmation.rideId}, driverId=${confirmation.driverId}")
            return false
        }

        session.lastUpdateAt = LocalDateTime.now()

        if (confirmation.accepted) {
            logger.info("Driver ${confirmation.driverId} accepted matching for ride ${confirmation.rideId}")

            val driver = session.availableDrivers.find { it.driverId == confirmation.driverId }
            if (driver != null) {
                // Update driver status to RIDING_CURRENTLY
                driverRepository.saveDriverStatus(
                    driverId = driver.driverId,
                    status = DriverStatus.RIDING_CURRENTLY,
                )

                session.status = MatchingStatus.COMPLETED
                session.result = MatchingResult(
                    driverId = driver.driverId,
                    acceptedAt = LocalDateTime.now()
                )

                publishDriverMatched(session, driver)

                activeMatchings.remove(confirmation.rideId)
            }
        } else {
            logger.info("Driver ${confirmation.driverId} declined matching for ride ${confirmation.rideId}")
            driverRepository.deleteDriverStatus(confirmation.driverId)
            session.driverResponded = true
            session.lastDriverAccepted = false
        }

        return true
    }

    fun getMatchingStatus(rideId: Long): MatchingStatusResponse? {
        val session = activeMatchings[rideId] ?: return null

        return MatchingStatusResponse(
            rideId = session.request.rideId,
            status = session.status,
            currentDriverId = session.currentDriverId,
            attemptedDrivers = session.attemptedDrivers,
            startedAt = session.startedAt,
            lastUpdateAt = session.lastUpdateAt,
            result = session.result
        )
    }

    suspend fun cancelMatching(rideId: Long): Boolean {
        val session = activeMatchings[rideId] ?: return false

        session.currentDriverId?.let { driverId ->
            driverRepository.deleteDriverStatus(driverId)
        }

        session.status = MatchingStatus.CANCELLED
        session.lastUpdateAt = LocalDateTime.now()
        activeMatchings.remove(rideId)

        logger.info("Cancelled matching for ride $rideId")
        return true
    }

    private suspend fun performMatching(session: MatchingSession) = coroutineScope {
        logger.info("Starting driver search for ride ${session.request.rideId}")

        val nearbyDrivers = getNearbyDrivers(session.request)

        if (nearbyDrivers.isEmpty()) {
            logger.warn("No drivers found for ride ${session.request.rideId}")
            session.status = MatchingStatus.NO_DRIVERS_AVAILABLE
            publishMatchingFailed(session, "No drivers available in the area")
            activeMatchings.remove(session.request.rideId)
            return@coroutineScope
        }

        session.availableDrivers = nearbyDrivers
        logger.info("Found ${nearbyDrivers.size} drivers for ride ${session.request.rideId}")

        // Try each driver
        for (driver in nearbyDrivers) {
            if (session.status == MatchingStatus.CANCELLED) {
                logger.info("Matching ${session.request.rideId} was cancelled")
                break
            }

            // Check if driver is already busy
            val driverStatus = driverRepository.getDriverStatus(driver.driverId)
            if (driverStatus != null) {
                logger.info("Driver ${driver.driverId} is already busy with status ${driverStatus.status}, skipping")
                continue
            }

            // Get driver connection ID from read-only table
            val connectionId = driverRepository.getDriverConnection(driver.driverId)
            if (connectionId == null) {
                logger.warn("No connection ID found for driver ${driver.driverId}, skipping")
                continue
            }

            session.currentDriverId = driver.driverId
            session.attemptedDrivers++
            session.status = MatchingStatus.WAITING_CONFIRMATION
            session.driverResponded = false
            session.lastUpdateAt = LocalDateTime.now()

            // Save driver status as PENDING_REQUEST
            driverRepository.saveDriverStatus(
                driverId = driver.driverId,
                status = DriverStatus.PENDING_REQUEST,
            )

            logger.info("Notifying driver ${driver.driverId} for ride ${session.request.rideId}")

            // Notify driver with connectionId
            val notificationRequest = DriverNotificationRequest(
                driverId = driver.driverId,
                rideId = session.request.rideId,
                pickupLongitude = session.request.pickupLongitude,
                pickupLatitude = session.request.pickupLatitude,
                dropoffLongitude = session.request.pickupLongitude,
                dropoffLatitude = session.request.dropoffLatitude,
                estimatedPrice = session.request.estimatedPrice,
            )

            try {
                messagingServiceClient.notifyDriver(notificationRequest, connectionId)

                // Wait for driver confirmation
                val confirmed = waitForDriverConfirmation(session)

                if (confirmed) {
                    // Driver accepted, matching complete
                    return@coroutineScope
                } else {
                    // Timeout - remove driver status
                    driverRepository.deleteDriverStatus(driver.driverId)
                }

                // Driver declined or timeout, try next
                logger.info("Driver ${driver.driverId} did not accept, trying next driver")

            } catch (e: Exception) {
                logger.error("Error notifying driver ${driver.driverId}", e)
                driverRepository.deleteDriverStatus(driver.driverId)
            }

            // Small delay before trying next driver
            if (nearbyDrivers.indexOf(driver) < nearbyDrivers.size - 1) {
                delay(delayBetweenAttempts)
            }
        }

        // No driver accepted
        logger.warn("No driver accepted for ride ${session.request.rideId}")
        session.status = MatchingStatus.NO_DRIVERS_AVAILABLE
        publishMatchingFailed(session, "No drivers accepted the ride request")
        activeMatchings.remove(session.request.rideId)
    }

private suspend fun waitForDriverConfirmation(session: MatchingSession): Boolean {
    val timeoutMillis = confirmationTimeout * 1000
    val startTime = System.currentTimeMillis()

    while (System.currentTimeMillis() - startTime < timeoutMillis) {
        if (session.driverResponded) {
            return session.lastDriverAccepted
        }

        if (session.status == MatchingStatus.CANCELLED || session.status == MatchingStatus.COMPLETED) {
            return session.status == MatchingStatus.COMPLETED
        }

        delay(100) // Check every 100ms
    }

        logger.info("Timeout waiting for driver ${session.currentDriverId} confirmation")
        return false
    }

private suspend fun getNearbyDrivers(request: MatchingRequest): List<NearbyDriver> {
    val nearbyRequest = NearbyDriversRequest(
        latitude = request.pickupLatitude,
        longitude = request.pickupLongitude,
        radius = searchRadius,
        limit = maxDriversToTry * 2
    )

    return locationServiceClient.getNearbyDrivers(nearbyRequest)
        .filter { it.isActive }
        .sortedBy { it.distance }
        .take(maxDriversToTry)
}

private fun publishDriverMatched(session: MatchingSession, driver: NearbyDriver) {
    val event = DriverMatchedEvent(
        rideId = session.request.rideId,
        driverId = driver.driverId,
        matchedAt = LocalDateTime.now(),
    )

    rabbitTemplate.convertAndSend(exchangeName, routingKey, event)
    logger.info("Published driver matched event: $event")
}

private fun publishMatchingFailed(session: MatchingSession, reason: String) {
    val event = MatchingFailedEvent(
        rideId = session.request.rideId,
        reason = reason,
        failedAt = LocalDateTime.now()
    )

    rabbitTemplate.convertAndSend(exchangeName, "$routingKey.failed", event)
    logger.info("Published matching failed event: $event")
}
}