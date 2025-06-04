package org.taxiapp.matching.service

import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.stereotype.Component
import org.taxiapp.matching.dto.events.`in`.MatchingCancelledEvent

@Component
class MatchingEventListener(private val driverMatchingService: DriverMatchingService) {
    private val logger = LoggerFactory.getLogger(RideEventListener::class.java)

    @RabbitListener(queues = ["\${rabbitmq.queue.driver-matching}"])
    fun handleMatchingCancelled(event: MatchingCancelledEvent) {
        logger.info("Received matching cancelled event: $event")

        event.rideId.let { rideId ->
            runBlocking {
                driverMatchingService.cancelMatching(rideId)
            }
        }
    }
}