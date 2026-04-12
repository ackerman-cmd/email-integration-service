package com.base.emailintegrationservice.integration.resend.dto

data class ResendEmailContent(
    val id: String,
    val html: String?,
    val text: String?,
)
