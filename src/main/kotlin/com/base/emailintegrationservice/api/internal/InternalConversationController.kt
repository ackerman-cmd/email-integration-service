package com.base.emailintegrationservice.api.internal

import com.base.emailintegrationservice.api.internal.dto.*
import com.base.emailintegrationservice.repository.ConversationRepository
import com.base.emailintegrationservice.repository.MessageAttachmentRepository
import com.base.emailintegrationservice.repository.MessageRecipientRepository
import com.base.emailintegrationservice.repository.MessageRepository
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
    private val messageAttachmentRepository: MessageAttachmentRepository
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
        return messages.map { message ->
            val recipients = messageRecipientRepository.findByMessageId(message.id)
            MessageResponse.from(message, recipients)
        }
    }
}

@Tag(name = "Internal — Conversations")
@RestController
@RequestMapping("/internal/messages")
class InternalMessageController(
    private val messageRepository: MessageRepository,
    private val messageAttachmentRepository: MessageAttachmentRepository
) {
    @Operation(
        summary = "Вложения сообщения",
        description = "Метаданные вложений. Физический файл — в S3/MinIO по storage_key"
    )
    @GetMapping("/{messageId}/attachments")
    fun getAttachments(
        @PathVariable messageId: UUID
    ): List<AttachmentResponse> {
        messageRepository
            .findById(messageId)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Message not found: $messageId") }
        return messageAttachmentRepository.findByMessageId(messageId).map { AttachmentResponse.from(it) }
    }
}
