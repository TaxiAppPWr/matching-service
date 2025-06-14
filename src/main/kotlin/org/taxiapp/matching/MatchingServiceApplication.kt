package org.taxiapp.matching

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.web.client.RestTemplate

@SpringBootApplication
class MatchingServiceApplication

fun main(args: Array<String>) {
    runApplication<MatchingServiceApplication>(*args)
}
