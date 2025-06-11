package org.taxiapp.matching.repository

import aws.sdk.kotlin.services.dynamodb.DynamoDbClient
import aws.sdk.kotlin.services.dynamodb.model.AttributeValue
import aws.sdk.kotlin.services.dynamodb.model.DeleteItemRequest
import aws.sdk.kotlin.services.dynamodb.model.GetItemRequest
import aws.sdk.kotlin.services.dynamodb.model.PutItemRequest
import aws.sdk.kotlin.services.dynamodb.model.QueryRequest
import aws.sdk.kotlin.services.dynamodb.query
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Repository
import org.taxiapp.matching.dto.dynamoDB.DriverStatus
import org.taxiapp.matching.dto.dynamoDB.DriverStatusRecord

@Repository
class DriverRepository(
    private val dynamoDbClient: DynamoDbClient,
    @Value("\${aws.dynamodb.driver-status-table}") private val driverStatusTable: String,
    @Value("\${aws.dynamodb.driver-connections-table}") private val driverConnectionsTable: String,
    @Value("\${aws.dynamodb.driver-connections-index}") private val driverIndexName: String
) {
    private val logger = LoggerFactory.getLogger(DriverRepository::class.java)

    suspend fun saveDriverStatus(driverId: String, status: DriverStatus) {
        val item = mutableMapOf(
            "driverId" to AttributeValue.S(driverId),
            "status" to AttributeValue.S(status.name)
        )

        val request = PutItemRequest {
            tableName = driverStatusTable
            this.item = item
        }

        dynamoDbClient.putItem(request)
        logger.info("Saved driver status: driverId=$driverId, status=$status")
    }

    suspend fun getDriverStatus(driverId: String): DriverStatusRecord? {
        val request = GetItemRequest {
            tableName = driverStatusTable
            key = mapOf("driverId" to AttributeValue.S(driverId))
        }

        val response = dynamoDbClient.getItem(request)
        val item = response.item

        return if (!item.isNullOrEmpty()) {
            DriverStatusRecord(
                driverId = item["driverId"]?.asS() ?: "",
                status = DriverStatus.mapToDriverStatus(item["status"]?.asS() ?: ""),
            )
        } else {
            null
        }
    }

    suspend fun deleteDriverStatus(driverId: String) {
        val request = DeleteItemRequest {
            tableName = driverStatusTable
            key = mapOf("driverId" to AttributeValue.S(driverId))
        }

        dynamoDbClient.deleteItem(request)
        logger.info("Deleted driver status for driverId=$driverId")
    }

    // Read-only method for driver connections
    suspend fun getDriverConnection(driverId: String): String? {
        val request = QueryRequest {
            indexName = driverIndexName
            tableName = driverConnectionsTable
            keyConditionExpression = "driverid = :driverId"
            expressionAttributeValues = mapOf("driverId" to AttributeValue.S(driverId))
        }

        val response = dynamoDbClient.query(request)
        val items = response.items

        return if (items.isNullOrEmpty())
            null
        else
            items[0]["connectionId"]?.asS()
    }
}