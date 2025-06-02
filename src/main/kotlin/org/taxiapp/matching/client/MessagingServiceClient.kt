package org.taxiapp.matching.client

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.taxiapp.matching.dto.driver.DriverNotificationRequest

@Component
class MessagingServiceClient(
    webClientBuilder: WebClient.Builder,
    @Value("\${services.messaging-service.base-url}") private val baseUrl: String
) {
    private val webClient = webClientBuilder.baseUrl(baseUrl).build()
    private val logger = LoggerFactory.getLogger(MessagingServiceClient::class.java)

    suspend fun notifyDriver(request: DriverNotificationRequest, connectionId: String) {
        // TODO implement proper driver call
        logger.info("Calling API Gateway at POST https://{api-id}.execute-api.us-east-1.amazonaws.com/{stage}/@connections/${connectionId}")
        webClient.post()
            .uri("/notifications/driver")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .retrieve()
            .bodyToMono(Void::class.java)
            .subscribe()
    }
}