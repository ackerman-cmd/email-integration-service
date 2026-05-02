package com.base.emailintegrationservice.config

import com.resend.Resend
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.web.client.RestClient

@Configuration
class ResendClientConfig(
    private val resendProperties: ResendProperties,
) {
    @Bean
    fun resend(): Resend = Resend(resendProperties.api.key)

    @Bean("resendRestClient")
    fun resendRestClient(): RestClient {
        val factory = SimpleClientHttpRequestFactory().apply {
            setConnectTimeout(10_000)
            setReadTimeout(120_000)
        }
        return RestClient
            .builder()
            .baseUrl(resendProperties.baseUrl)
            .defaultHeader("Authorization", "Bearer ${resendProperties.api.key}")
            .requestFactory(factory)
            .build()
    }
}
