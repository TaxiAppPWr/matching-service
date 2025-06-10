package org.taxiapp.matching.client

import aws.sdk.kotlin.services.apigatewaymanagementapi.ApiGatewayManagementClient
import aws.sdk.kotlin.services.apigatewaymanagementapi.model.PostToConnectionRequest
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.taxiapp.matching.dto.driver.DriverCancellationNotification
import org.taxiapp.matching.dto.driver.DriverRideOfferNotification


@Component
class NotificationServiceClient(
    private val mapper: ObjectMapper,
    private val apiManagementClient: ApiGatewayManagementClient,
) {
    private val logger = LoggerFactory.getLogger(NotificationServiceClient::class.java)

    suspend fun sendRidePropositionNotification(request: DriverRideOfferNotification, connectionId: String) {
        val data = mapper.writeValueAsString(request)
        logger.info("Sending ride proposition event to driver with connectionId: $connectionId")
        notifyDriver(data, connectionId)
    }

    suspend fun sendRideCancelledNotification(request: DriverCancellationNotification, connectionId: String) {
        val data = mapper.writeValueAsString(request)
        logger.info("Sending ride cancelled event to driver with connectionId: $connectionId")
        notifyDriver(data, connectionId)
    }

    private suspend fun notifyDriver(data: String, connectionId: String) {
        val request = PostToConnectionRequest {
            this.connectionId = connectionId
            this.data = data.toByteArray(Charsets.UTF_8)
        }
        try {
            apiManagementClient.postToConnection(request)
            logger.info("Notification sent to driver with connectionId: $connectionId")
        } catch (e: Exception) {
            logger.error("Failed to send notification to driver with connectionId: $connectionId", e)
        }
    }
}