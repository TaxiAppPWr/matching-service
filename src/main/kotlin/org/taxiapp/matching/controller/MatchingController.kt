package org.taxiapp.matching.controller

import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.taxiapp.matching.dto.matching.MatchingRequest
import org.taxiapp.matching.dto.matching.MatchingResponse
import org.taxiapp.matching.service.DriverMatchingService


@RestController
@RequestMapping("/api/matching")
class MatchingController(
    private val matchingService: DriverMatchingService
) {

    @PostMapping("/find-driver")
    suspend fun findDriver(@Valid @RequestBody request: MatchingRequest): ResponseEntity<MatchingResponse> {
        val response = matchingService.findDriver(request)
        return ResponseEntity.ok(response)
    }

    @DeleteMapping("/{matchingId}")
    suspend fun cancelMatching(@PathVariable matchingId: String): ResponseEntity<Map<String, Any>> {
        val cancelled = matchingService.cancelMatching(matchingId)
        return if (cancelled) {
            ResponseEntity.ok(mapOf(
                "matchingId" to matchingId,
                "status" to "CANCELLED",
                "message" to "Matching process cancelled successfully"
            ))
        } else {
            ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf(
                "error" to "Matching session not found",
                "matchingId" to matchingId
            ))
        }
    }

    @GetMapping("/{matchingId}/status")
    fun getMatchingStatus(@PathVariable matchingId: String): ResponseEntity<Any> {
        val session = matchingService.getMatchingStatus(matchingId)
        return if (session != null) {
            ResponseEntity.ok(mapOf(
                "matchingId" to session.matchingId,
                "rideId" to session.request.rideId,
                "status" to "IN_PROGRESS",
                "attemptedDrivers" to session.attempts.size,
                "elapsedTimeMs" to (System.currentTimeMillis() - session.startTime)
            ))
        } else {
            ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf(
                "error" to "Matching session not found",
                "matchingId" to matchingId
            ))
        }
    }

    @GetMapping("/health")
    fun health(): ResponseEntity<Map<String, String>> {
        return ResponseEntity.ok(mapOf(
            "status" to "UP",
            "service" to "driver-matching-service"
        ))
    }
}