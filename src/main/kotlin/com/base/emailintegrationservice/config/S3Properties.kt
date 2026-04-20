package com.base.emailintegrationservice.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.s3")
data class S3Properties(
    val endpoint: String = "http://localhost:9100",
    val accessKey: String = "minioadmin",
    val secretKey: String = "minioadmin",
    val bucket: String = "email-attachments",
    val region: String = "us-east-1",
    val publicUrl: String = "",
)
