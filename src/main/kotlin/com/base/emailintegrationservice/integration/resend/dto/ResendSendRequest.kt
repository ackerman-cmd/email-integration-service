package com.base.emailintegrationservice.integration.resend.dto

data class ResendSendRequest(
    val from: String,
    val to: List<String>,
    val subject: String,
    val html: String? = null,
    val text: String? = null,
    val cc: List<String>? = null,
    val bcc: List<String>? = null,
    val replyTo: List<String>? = null,
    val headers: Map<String, String>? = null,
    val attachments: List<ResendAttachmentRequest>? = null,
    val tags: List<ResendTag>? = null
)

data class ResendAttachmentRequest(
    val filename: String,
    val content: String,
    val contentType: String? = null
)

data class ResendTag(
    val name: String,
    val value: String
)
