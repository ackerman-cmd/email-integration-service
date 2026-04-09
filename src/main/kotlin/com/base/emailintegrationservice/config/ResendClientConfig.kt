package com.base.emailintegrationservice.config

import com.resend.Resend
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class ResendClientConfig(
    private val resendProperties: ResendProperties,
) {
    @Bean
    fun resend(): Resend = Resend(resendProperties.api.key)
}
