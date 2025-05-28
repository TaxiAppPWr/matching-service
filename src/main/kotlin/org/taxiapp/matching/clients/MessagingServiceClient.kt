package org.taxiapp.matching.clients

import kotlinx.coroutines.reactive.awaitSingle
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.taxiapp.matching.dto.driver.DriverNotificationRequest
import org.taxiapp.matching.dto.driver.DriverNotificationResponse
import reactor.core.publisher.Mono
import java.time.Duration

@Component
class MessagingServiceClient(
    webClientBuilder: WebClient.Builder,
    @Value("\${services.messaging-service.base-url}") private val baseUrl: String
) {
    private val webClient = webClientBuilder
        .baseUrl(baseUrl)
        .build()

    suspend fun notifyDriver(request: DriverNotificationRequest): DriverNotificationResponse {
        return webClient.post()
            .uri("/notifications/driver")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .retrieve()
            .bodyToMono(DriverNotificationResponse::class.java)
            .timeout(Duration.ofMillis(request.timeoutMs))
            .onErrorResume { error ->
                Mono.just(
                    DriverNotificationResponse(
                    notificationId = "timeout-${request.driverId}",
                    driverId = request.driverId,
                    accepted = false,
                    responseTimeMs = request.timeoutMs,
                    reason = "Timeout or error: ${error.message}"
                )
                )
            }
            .awaitSingle()
    }
}