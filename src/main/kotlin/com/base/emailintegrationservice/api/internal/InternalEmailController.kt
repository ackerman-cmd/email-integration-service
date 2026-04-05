package com.base.emailintegrationservice.api.internal

import com.base.emailintegrationservice.api.internal.dto.*
import com.base.emailintegrationservice.service.OutboundEmailService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

/**
 * Internal API consumed by arm-support-service.
 * All endpoints speak in business terms — arm-support-service has no knowledge of Resend internals.
 */
@Tag(name = "Internal — Emails")
@RestController
@RequestMapping("/internal/emails")
class InternalEmailController(
    private val outboundEmailService: OutboundEmailService
) {
    @Operation(
        summary = "Создать новое письмо",
        description = "Создаёт новый conversation и отправляет письмо через Resend. " +
            "Ящик задаётся полем fromEmail (адрес как в БД), без UUID."
    )
    @PostMapping("/send")
    @ResponseStatus(HttpStatus.CREATED)
    fun sendNew(
        @Valid @RequestBody request: SendEmailRequest
    ): MessageCreatedResponse {
        val message = outboundEmailService.sendNew(
            fromEmail = request.fromEmail,
            to = request.to,
            cc = request.cc,
            subject = request.subject,
            htmlBody = request.htmlBody,
            textBody = request.textBody,
            createdByUserId = request.createdByUserId
        )
        return MessageCreatedResponse(
            messageId = message.id,
            conversationId = message.conversation.id,
            status = message.status
        )
    }

    @Operation(
        summary = "Ответить в цепочку",
        description = "Добавляет ответ в существующий conversation. In-Reply-To и References строятся автоматически."
    )
    @PostMapping("/reply")
    @ResponseStatus(HttpStatus.CREATED)
    fun reply(
        @Valid @RequestBody request: ReplyEmailRequest
    ): MessageCreatedResponse {
        val message = outboundEmailService.reply(
            conversationId = request.conversationId,
            replyToMessageId = request.replyToMessageId,
            to = request.to,
            cc = request.cc,
            htmlBody = request.htmlBody,
            textBody = request.textBody,
            createdByUserId = request.createdByUserId
        )
        return MessageCreatedResponse(
            messageId = message.id,
            conversationId = message.conversation.id,
            status = message.status
        )
    }

    @Operation(summary = "Переслать письмо", description = "Пересылает выбранное сообщение с оригинальным контентом")
    @PostMapping("/forward")
    @ResponseStatus(HttpStatus.CREATED)
    fun forward(
        @Valid @RequestBody request: ForwardEmailRequest
    ): MessageCreatedResponse {
        val message = outboundEmailService.forward(
            messageId = request.messageId,
            to = request.to,
            note = request.note,
            createdByUserId = request.createdByUserId
        )
        return MessageCreatedResponse(
            messageId = message.id,
            conversationId = message.conversation.id,
            status = message.status
        )
    }
}
