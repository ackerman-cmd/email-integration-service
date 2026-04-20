package com.base.emailintegrationservice.api.internal

import com.base.emailintegrationservice.api.internal.dto.*
import com.base.emailintegrationservice.repository.ConversationRepository
import com.base.emailintegrationservice.repository.MessageAttachmentRepository
import com.base.emailintegrationservice.repository.MessageRecipientRepository
import com.base.emailintegrationservice.repository.MessageRepository
import com.base.emailintegrationservice.service.S3StorageService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

@Tag(name = "Internal — Conversations")
@RestController
@RequestMapping("/internal/conversations")
class InternalConversationController(
    private val conversationRepository: ConversationRepository,
    private val messageRepository: MessageRepository,
    private val messageRecipientRepository: MessageRecipientRepository,
    private val messageAttachmentRepository: MessageAttachmentRepository,
    private val s3StorageService: S3StorageService,
) {
    @Operation(summary = "Получить conversation", description = "Метаданные переписки по ID")
    @GetMapping("/{conversationId}")
    fun getConversation(
        @PathVariable conversationId: UUID
    ): ConversationResponse {
        val conversation = conversationRepository
            .findById(conversationId)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Conversation not found: $conversationId") }
        return ConversationResponse.from(conversation)
    }

    @Operation(summary = "Список сообщений", description = "Все сообщения conversation в хронологическом порядке")
    @GetMapping("/{conversationId}/messages")
    fun getMessages(
        @PathVariable conversationId: UUID
    ): List<MessageResponse> {
        conversationRepository
            .findById(conversationId)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Conversation not found: $conversationId") }

        val messages = messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId)
        val messageIds = messages.map { it.id }
        val attachmentsByMessageId = messageAttachmentRepository
            .findByMessageIdIn(messageIds)
            .groupBy { it.message.id }
        return messages.map { message ->
            val recipients = messageRecipientRepository.findByMessageId(message.id)
            val attachments = (attachmentsByMessageId[message.id] ?: emptyList())
                .filterNot { it.isInline }
                .map { a -> AttachmentResponse.from(a, a.storageKey?.let { s3StorageService.buildUrl(it) }) }
            MessageResponse.from(message, recipients, attachments)
        }
    }
}

@Tag(name = "Internal — Conversations")
@RestController
@RequestMapping("/internal/messages")
class InternalMessageController(
    private val messageRepository: MessageRepository,
    private val messageAttachmentRepository: MessageAttachmentRepository,
    private val s3StorageService: S3StorageService,
) {
    @Operation(
        summary = "Вложения сообщения",
        description = "Метаданные вложений. Физический файл — в S3/MinIO по storageUrl"
    )
    @GetMapping("/{messageId}/attachments")
    fun getAttachments(
        @PathVariable messageId: UUID
    ): List<AttachmentResponse> {
        messageRepository
            .findById(messageId)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Message not found: $messageId") }
        return messageAttachmentRepository.findByMessageId(messageId).map { attachment ->
            val storageUrl = attachment.storageKey?.let { s3StorageService.buildUrl(it) }
            AttachmentResponse.from(attachment, storageUrl)
        }
    }
}
