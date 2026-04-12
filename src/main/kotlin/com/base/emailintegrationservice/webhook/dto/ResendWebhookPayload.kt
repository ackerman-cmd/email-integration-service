package com.base.emailintegrationservice.webhook.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class ResendWebhookPayload(
    val type: String,
    val createdAt: String?,
    val data: ResendWebhookData,
)

data class ResendWebhookData(
    @JsonProperty("email_id") val emailId: String?,
    // RFC 2822 Message-ID of the incoming email — provided by Resend as a top-level field,
    // NOT inside the headers[] array (which Resend does not include in email.received webhooks).
    @JsonProperty("message_id") val messageId: String?,
    val from: String?,
    val to: List<String>?,
    val cc: List<String>?,
    val bcc: List<String>?,
    @JsonProperty("reply_to") val replyTo: List<String>?,
    val subject: String?,
    // Note: Resend does NOT include html/text body in email.received webhooks.
    // These fields will always be null for inbound emails.
    val html: String?,
    val text: String?,
    // Note: Resend does NOT include headers[] in email.received webhooks.
    // In-Reply-To / References are therefore unavailable; threading uses subject fallback.
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
