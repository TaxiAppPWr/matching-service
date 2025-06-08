package org.taxiapp.matching.config

import aws.sdk.kotlin.runtime.auth.credentials.StaticCredentialsProvider
import aws.sdk.kotlin.services.dynamodb.DynamoDbClient
import kotlinx.coroutines.runBlocking
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class DynamoDbConfig(
    @Value("\${aws.dynamodb.region}") private val region: String,
    @Value("\${aws.access_key}") private val awsAccessKey: String,
    @Value("\${aws.secret_access_key}") private val awsSecretAccessKey: String
) {

    @Bean
    fun dynamoDbClient(): DynamoDbClient = runBlocking {
        DynamoDbClient {
            this.region = this@DynamoDbConfig.region
        }
    }
}