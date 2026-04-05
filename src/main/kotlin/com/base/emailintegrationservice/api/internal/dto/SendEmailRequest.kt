package com.base.emailintegrationservice.api.internal.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import java.util.UUID

/**
 * @param fromEmail Полный адрес ящика-отправителя (`support@company.com`), как в [Mailbox.emailAddress].
 *   UUID ящика с фронта не нужен.
 */
data class SendEmailRequest(
    @field:NotBlank @field:Email val fromEmail: String,
    @field:NotEmpty val to: List<@Email String>,
    val cc: List<String> = emptyList(),
    @field:NotBlank val subject: String,
    val htmlBody: String? = null,
    val textBody: String? = null,
    val createdByUserId: UUID? = null
)

data class ReplyEmailRequest(
    val conversationId: UUID,
    val replyToMessageId: UUID? = null,
    @field:NotEmpty val to: List<@Email String>,
    val cc: List<String> = emptyList(),
    val htmlBody: String? = null,
    val textBody: String? = null,
    val createdByUserId: UUID? = null
)

data class ForwardEmailRequest(
    val messageId: UUID,
    @field:NotEmpty val to: List<@Email String>,
    val note: String? = null,
    val createdByUserId: UUID? = null
)
