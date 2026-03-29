package com.base.emailintegrationservice.webhook.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class ResendWebhookPayload(
    val type: String,
    val createdAt: String?,
    val data: ResendWebhookData,
)

data class ResendWebhookData(
    @JsonProperty("email_id") val emailId: String?,
    val from: String?,
    val to: List<String>?,
    val cc: List<String>?,
    val bcc: List<String>?,
    val replyTo: List<String>?,
    val subject: String?,
    val html: String?,
    val text: String?,
    val headers: List<ResendEmailHeader>?,
    val attachments: List<ResendWebhookAttachment>?,
)

data class ResendEmailHeader(
    val name: String,
    val value: String,
)

data class ResendWebhookAttachment(
    val id: String?,
    val filename: String?,
    val contentType: String?,
    val size: Long?,
)
