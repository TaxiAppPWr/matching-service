package org.taxiapp.matching.service

import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.stereotype.Component
import org.taxiapp.matching.dto.events.`in`.RideCancelledEvent
import org.taxiapp.matching.dto.events.`in`.RideFinishedEvent
import org.taxiapp.matching.repository.DriverRepository

@Component
class RideEventListener(
    private val driverRepository: DriverRepository,
    private val driverMatchingService: DriverMatchingService
) {
    private val logger = LoggerFactory.getLogger(RideEventListener::class.java)

    @RabbitListener(queues = ["\${rabbitmq.queue.matching}"])
    fun handleRideFinished(event: RideFinishedEvent) {
        logger.info("Received ride finished event: $event")

        event.driverId.let { driverId ->
            runBlocking {
                driverRepository.deleteDriverStatus(driverId)
            }
            logger.info("Removed driver $driverId from active rides after ride ${event.rideId} finished")
        }
    }

    @RabbitListener(queues = ["\${rabbitmq.queue.matching}"])
    fun handleCancelledFinished(event: RideCancelledEvent) {
        logger.info("Received ride cancelled event: $event")

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