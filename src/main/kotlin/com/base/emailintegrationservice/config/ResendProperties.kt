package com.base.emailintegrationservice.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "resend")
data class ResendProperties(
    val api: Api = Api(),
    val baseUrl: String = "https://api.resend.com",
    val webhookSecret: String = ""
) {
    data class Api(
        val key: String = ""
    )
}
