package org.taxiapp.matching.messaging

import org.springframework.amqp.rabbit.annotation.RabbitHandler
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.stereotype.Component
import org.taxiapp.matching.dto.events.CancelRideEvent
import org.taxiapp.matching.dto.events.RideFinishedEvent
import org.taxiapp.matching.service.DriverMatchingService

@RabbitListener(queues = ["\${rabbitmq.queue.driver-matching}"])
@Component
class MessageReceiver(
    private val matchingService: DriverMatchingService,
) {

    @RabbitHandler
    fun receiveRideCanceledEvent(event: CancelRideEvent) {
        matchingService.cancelMatching(event.rideId)
    }

    @RabbitHandler
    fun receiveRideFinishedEvent(event: RideFinishedEvent) {
        TODO("not implemented")
    }


    @RabbitHandler(isDefault = true)
    fun receiveDefault(event: Any) {
        // Do nothing
    }
}