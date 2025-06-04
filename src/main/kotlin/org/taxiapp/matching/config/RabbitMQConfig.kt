package org.taxiapp.matching.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.springframework.amqp.core.*
import org.springframework.amqp.rabbit.connection.ConnectionFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.amqp.support.converter.DefaultJackson2JavaTypeMapper
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter
import org.springframework.amqp.support.converter.MessageConverter
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.taxiapp.matching.dto.events.CancelRideEvent
import org.taxiapp.matching.dto.events.RideFinishedEvent

@Configuration
class RabbitMQConfig(
    @Value("\${rabbitmq.exchange.driver-matching}") private val exchangeName: String,
    @Value("\${rabbitmq.exchange.ride}") private val rideExchangeName: String,
    @Value("\${rabbitmq.queue.matching}") private val queueName: String,
    @Value("\${rabbitmq.routing-key.driver-matching}") private val routingKey: String,
    @Value("\${rabbitmq.routing-key.ride.cancel}") private val rideCanceledKey: String,
    @Value("\${rabbitmq.routing-key.ride.finished}") private val rideFinishedKey: String
) {
    @Bean
    fun driverMatchedExchange(): DirectExchange {
        return DirectExchange(exchangeName)
    }

    @Bean
    fun rideExchange(): DirectExchange {
        return DirectExchange(rideExchangeName)
    }

    @Bean
    fun matchingServiceQueue(): Queue {
        return QueueBuilder.durable(queueName).build()
    }

    @Bean
    fun driverMatchedBinding(queue: Queue, @Qualifier("driverMatchedExchange") exchange: DirectExchange): Binding {
        return BindingBuilder.bind(queue).to(exchange).with(routingKey)
    }

    @Bean
    fun rideCanceledBinding(queue: Queue, @Qualifier("rideExchange") exchange: DirectExchange): Binding {
        return BindingBuilder.bind(queue).to(exchange).with(rideCanceledKey)
    }

    @Bean
    fun rideFinishedBinding(queue: Queue, @Qualifier("rideExchange") exchange: DirectExchange): Binding {
        return BindingBuilder.bind(queue).to(exchange).with(rideFinishedKey)
    }

    @Bean
    fun jsonMessageConverter(): MessageConverter {
        val mapper = ObjectMapper().apply {
            registerKotlinModule()
            registerModule(JavaTimeModule()).disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        }

        val typeMapper = DefaultJackson2JavaTypeMapper().apply {
            setTrustedPackages("*")
            idClassMapping = mapOf(
                "taxiapp.ride.dto.event.CancelRideEvent" to CancelRideEvent::class.java,
                "taxiapp.ride.dto.event.RideFinishedEvent" to RideFinishedEvent::class.java
            )
        }

        return Jackson2JsonMessageConverter(mapper).apply {
            javaTypeMapper = typeMapper
        }
    }

    @Bean
    fun rabbitTemplate(connectionFactory: ConnectionFactory, jsonMessageConverter: MessageConverter): RabbitTemplate {
        val rabbitTemplate = RabbitTemplate(connectionFactory)
        rabbitTemplate.messageConverter = jsonMessageConverter
        return rabbitTemplate
    }
}