package org.taxiapp.matching.client

import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.taxiapp.matching.dto.driver.DriverNotificationRequest
import org.taxiapp.matching.dto.driver.EmailSendRequest

@Component
class NotificationServiceClient(
    webClientBuilder: WebClient.Builder,
    @Value("\${services.notification-service.base-url}") private val baseUrl: String
) {
    private val webClient = webClientBuilder.baseUrl(baseUrl).build()

    suspend fun notifyDriver(request: DriverNotificationRequest) {
        // TODO - change this to push notification
        webClient.post()
            .uri("api/notification/email")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(EmailSendRequest("test@test.com", "proposed ride", "test"))
            .retrieve()
            .bodyToMono(Void::class.java)
            .subscribe()
    }
}