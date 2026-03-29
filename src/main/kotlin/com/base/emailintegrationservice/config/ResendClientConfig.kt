package com.base.emailintegrationservice.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestClient

@Configuration
class ResendClientConfig(
    private val resendProperties: ResendProperties
) {
    @Bean("resendRestClient")
    fun resendRestClient(): RestClient =
        RestClient
            .builder()
            .baseUrl(resendProperties.baseUrl)
            .defaultHeader("Authorization", "Bearer ${resendProperties.api.key}")
            .defaultHeader("Content-Type", "application/json")
            .build()
}
