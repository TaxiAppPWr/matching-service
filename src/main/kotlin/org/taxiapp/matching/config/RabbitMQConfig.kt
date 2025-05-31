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
    @Value("\${rabbitmq.exchange.driver-matching}") private val exchangeName: String,
    @Value("\${rabbitmq.queue.driver-matching}") private val queueName: String,
    @Value("\${rabbitmq.routing-key.driver-matching}") private val routingKey: String
) {

    @Bean
    fun driverMatchedExchange(): DirectExchange {
        return DirectExchange(exchangeName)
    }

    @Bean
    fun driverMatchedQueue(): Queue {
        return QueueBuilder.durable(queueName).build()
    }

    @Bean
    fun driverMatchedBinding(queue: Queue, exchange: DirectExchange): Binding {
        return BindingBuilder.bind(queue).to(exchange).with(routingKey)
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