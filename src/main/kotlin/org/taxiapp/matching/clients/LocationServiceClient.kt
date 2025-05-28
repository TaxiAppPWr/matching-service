package org.taxiapp.matching.clients

import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.stereotype.Component
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.taxiapp.matching.dto.driver.NearbyDriver
import org.taxiapp.matching.dto.driver.NearbyDriversRequest
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody
import java.time.Duration
import java.time.LocalDateTime

@Component
class LocationServiceClient(
    webClientBuilder: WebClient.Builder,
    @Value("\${services.location-service.base-url}") private val baseUrl: String,
    @Value("\${services.location-service.timeout}") private val timeout: Long
) {
    private val webClient = webClientBuilder
        .baseUrl(baseUrl)
        .build()

    suspend fun getNearbyDrivers(request: NearbyDriversRequest): List<NearbyDriver> {
        return webClient.post()
            .uri("/drivers/nearby")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .retrieve()
            .bodyToMono(List::class.java)
            .timeout(Duration.ofMillis(timeout))
            .map { list ->
                @Suppress("UNCHECKED_CAST")
                (list as List<Map<String, Any>>).map { map ->
                    NearbyDriver(
                        distance = (map["distance"] as Number).toDouble(),
                        id = (map["id"] as Number).toLong(),
                        isActive = map["isActive"] as Boolean,
                        lastPing = LocalDateTime.parse(map["lastPing"] as String),
                        latitude = (map["latitude"] as Number).toDouble(),
                        longitude = (map["longitude"] as Number).toDouble(),
                    )
                }
            }
            .awaitSingle()
    }
}
