package org.taxiapp.matching.service

import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.annotation.RabbitHandler
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.stereotype.Component
import org.taxiapp.matching.client.NotificationServiceClient
import org.taxiapp.matching.dto.driver.DriverCancellationNotification
import org.taxiapp.matching.dto.events.`in`.RideCancelledEvent
import org.taxiapp.matching.dto.events.`in`.RideFinishedEvent
import org.taxiapp.matching.repository.DriverRepository

@Component
@RabbitListener(queues = ["\${rabbitmq.queue.matching}"])
class RideEventListener(
    private val driverRepository: DriverRepository,
    private val driverMatchingService: DriverMatchingService,
    private val notificationServiceClient: NotificationServiceClient
) {
    private val logger = LoggerFactory.getLogger(RideEventListener::class.java)

    @RabbitHandler
    fun handleRideFinished(event: RideFinishedEvent) {
        logger.info("Received ride finished event: $event")

        event.driverUsername.let { driverId ->
            runBlocking {
                driverRepository.deleteDriverStatus(driverId)
            }
            logger.info("Removed driver $driverId from active rides after ride ${event.rideId} finished")
        }
    }

    @RabbitHandler
    fun handleCancelledFinished(event: RideCancelledEvent) {
        logger.info("Received ride cancelled event: $event")

        runBlocking {
            val connectionId = driverRepository.getDriverConnection(event.driverId)
            connectionId?.let { connectionId ->
                val request = DriverCancellationNotification(
                    driverId = event.driverId,
                    rideId = event.rideId
                )
                notificationServiceClient.sendRideCancelledNotification(
                    request = request,
                    connectionId = connectionId
                )
            }
        }

        event.driverId.let { driverId ->
            runBlocking {
                driverRepository.deleteDriverStatus(driverId)
            }
            logger.info("Removed driver $driverId from active rides after ride ${event.rideId} cancelled")
        }

        event.rideId.let { rideId ->
            runBlocking {
                driverMatchingService.cancelMatching(rideId)
            }
        }
    }
}