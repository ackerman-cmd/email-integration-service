package com.base.emailintegrationservice.config

import com.resend.Resend
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.JdkClientHttpRequestFactory
import org.springframework.web.client.RestClient
import java.net.http.HttpClient
import java.time.Duration

@Configuration
class ResendClientConfig(
    private val resendProperties: ResendProperties,
) {
    @Bean
    fun resend(): Resend = Resend(resendProperties.api.key)

    @Bean("resendRestClient")
    fun resendRestClient(): RestClient {
        val httpClient = HttpClient
            .newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .version(HttpClient.Version.HTTP_2)
            .build()

        val factory = JdkClientHttpRequestFactory(httpClient).apply {
            setReadTimeout(Duration.ofSeconds(120))
        }

        return RestClient
            .builder()
            .baseUrl(resendProperties.baseUrl)
            .defaultHeader("Authorization", "Bearer ${resendProperties.api.key}")
            .requestFactory(factory)
            .build()
    }
}
