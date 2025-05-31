package org.taxiapp.matching.client

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

    suspend fun notifyDriver(request: DriverNotificationRequest) {
        webClient.post()
            .uri("/notifications/driver")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .retrieve()
            .bodyToMono(Void::class.java)
            .subscribe()
    }
}