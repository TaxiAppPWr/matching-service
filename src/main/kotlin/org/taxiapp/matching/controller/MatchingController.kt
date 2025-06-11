package org.taxiapp.matching.controller

import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.taxiapp.matching.dto.driver.DriverConfirmation
import org.taxiapp.matching.dto.driver.DriverConfirmationRequest
import org.taxiapp.matching.dto.matching.MatchingRequest
import org.taxiapp.matching.dto.matching.MatchingStartedResponse
import org.taxiapp.matching.repository.DriverRepository
import org.taxiapp.matching.service.DriverMatchingService


@RestController
@RequestMapping("/api/matching")
class MatchingController(
    private val matchingService: DriverMatchingService,
    private val driverRepository: DriverRepository
) {

    @PostMapping("/find-driver")
    fun findDriver(@Valid @RequestBody request: MatchingRequest): ResponseEntity<MatchingStartedResponse> {
        val response = matchingService.startMatching(request)
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response)
    }

    @PostMapping("/confirm")
    suspend fun confirmDriver(@RequestHeader("username") userId: String, @Valid @RequestBody request: DriverConfirmationRequest): ResponseEntity<Map<String, Any>> {
        val confirmation = DriverConfirmation(request.rideId, userId, request.accepted)
        val confirmed = matchingService.confirmDriver(confirmation)

        return if (confirmed) {
            ResponseEntity.ok(mapOf(
                "success" to true,
                "message" to "Confirmation processed"
            ))
        } else {
            ResponseEntity.status(HttpStatus.BAD_REQUEST).body(mapOf(
                "success" to false,
                "error" to "Invalid confirmation"
            ))
        }
    }

    @GetMapping("/{rideId}/status")
    fun getMatchingStatus(@PathVariable rideId: Long): ResponseEntity<Any> {
        val status = matchingService.getMatchingStatus(rideId)

        return if (status != null) {
            ResponseEntity.ok(status)
        } else {
            ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf(
                "error" to "Matching not found",
                "rideId" to rideId
            ))
        }
    }

    @DeleteMapping("/{rideId}")
    suspend fun cancelMatching(@PathVariable rideId: Long): ResponseEntity<Map<String, Any>> {
        val cancelled = matchingService.cancelMatching(rideId)

        return if (cancelled) {
            ResponseEntity.ok(mapOf(
                "success" to true,
                "message" to "Matching cancelled"
            ))
        } else {
            ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf(
                "success" to false,
                "error" to "Matching not found"
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

    @GetMapping("/health/dynamodb")
    suspend fun checkDynamoDb(): ResponseEntity<Map<String, Any>> {
         try {
            driverRepository.getDriverStatus("")
             return ResponseEntity.ok(mapOf(
                "status" to "UP",
                "service" to "DynamoDB"
            ))
        } catch (e: Exception) {
             return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(mapOf(
                 "status" to "DOWN",
                 "service" to "DynamoDB",
                 "error" to e.message!!
             ))
        }
    }
}