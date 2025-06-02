package org.taxiapp.matching.config

import org.springframework.amqp.core.*
import org.springframework.amqp.rabbit.connection.ConnectionFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class RabbitMQConfig(
    @Value("\${rabbitmq.exchange.driver-matching}") private val driverMatchingExchange: String,
    @Value("\${rabbitmq.queue.driver-matching}") private val driverMatchingQueue: String,
    @Value("\${rabbitmq.routing-key.driver-matching}") private val driverMatchingRoutingKey: String,
    @Value("\${rabbitmq.exchange.ride-events}") private val rideEventsExchange: String,
    @Value("\${rabbitmq.queue.ride-events}") private val rideEventsQueue: String,
    @Value("\${rabbitmq.routing-key.ride-finished}") private val rideFinishedRoutingKey: String,
    @Value("\${rabbitmq.routing-key.ride-cancelled}") private val rideCancelledRoutingKey: String,
    @Value("\${rabbitmq.routing-key.matching-cancelled}") private val matchingCancelledRoutingKey: String
) {

    @Bean
    fun driverMatchingExchange(): DirectExchange {
        return DirectExchange(driverMatchingExchange)
    }

    @Bean
    fun rideEventsExchange(): TopicExchange {
        return TopicExchange(rideEventsExchange)
    }

    @Bean
    fun driverMatchingQueue(): Queue {
        return QueueBuilder.durable(driverMatchingQueue).build()
    }

    @Bean
    fun rideEventsQueue(): Queue {
        return QueueBuilder.durable(rideEventsQueue).build()
    }


    @Bean
    fun rideFinishedBinding(): Binding {
        return BindingBuilder.bind(rideEventsQueue())
            .to(rideEventsExchange())
            .with(rideFinishedRoutingKey)
    }

    @Bean
    fun rideCancelledBinding(): Binding {
        return BindingBuilder.bind(rideEventsQueue())
            .to(rideEventsExchange())
            .with(rideCancelledRoutingKey)
    }

    @Bean
    fun matchingCancelledBinding(): Binding {
        return BindingBuilder.bind(driverMatchingQueue())
            .to(driverMatchingExchange())
            .with(matchingCancelledRoutingKey)
    }

    @Bean
    fun messageConverter(): Jackson2JsonMessageConverter {
        return Jackson2JsonMessageConverter()
    }

    @Bean
    fun rabbitTemplate(connectionFactory: ConnectionFactory, converter: Jackson2JsonMessageConverter): RabbitTemplate {
        val template = RabbitTemplate(connectionFactory)
        template.messageConverter = converter
        return template
    }
}