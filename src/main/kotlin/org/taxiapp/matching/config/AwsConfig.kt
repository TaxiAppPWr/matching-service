package org.taxiapp.matching.config

import aws.sdk.kotlin.services.apigatewaymanagementapi.ApiGatewayManagementClient
import aws.smithy.kotlin.runtime.net.url.Url
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class AwsConfig(
    @Value("\${aws.region}") private val awsRegion: String,
    @Value("\${aws.websocket-api.id}") private val websocketApiId: String,
    @Value("\${aws.websocket-api.stage}") private val websocketApiStage: String
) {
    @Bean
    fun apiManagementClient(): ApiGatewayManagementClient {
        return ApiGatewayManagementClient {
            this.region = awsRegion
            this.endpointUrl =
                Url.parse("https://$websocketApiId.execute-api.$awsRegion.amazonaws.com/$websocketApiStage")
        }
    }
}
