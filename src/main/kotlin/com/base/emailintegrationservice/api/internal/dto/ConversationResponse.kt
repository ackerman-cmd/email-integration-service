package com.base.emailintegrationservice.api.internal.dto

import com.base.emailintegrationservice.domain.entity.Conversation
import com.base.emailintegrationservice.domain.entity.Message
import com.base.emailintegrationservice.domain.entity.MessageAttachment
import com.base.emailintegrationservice.domain.entity.MessageRecipient
import com.base.emailintegrationservice.domain.enums.ConversationStatus
import com.base.emailintegrationservice.domain.enums.MessageDirection
import com.base.emailintegrationservice.domain.enums.MessageStatus
import com.base.emailintegrationservice.domain.enums.RecipientType
import java.time.Instant
import java.util.UUID

data class ConversationResponse(
    val id: UUID,
    val mailboxId: UUID,
    val mailboxEmail: String,
    val subjectNormalized: String?,
    val caseId: UUID?,
    val status: ConversationStatus,
    val startedAt: Instant?,
    val lastMessageAt: Instant?,
    val createdAt: Instant
) {
    companion object {
        fun from(conversation: Conversation) =
            ConversationResponse(
                id = conversation.id,
                mailboxId = conversation.mailbox.id,
                mailboxEmail = conversation.mailbox.emailAddress,
                subjectNormalized = conversation.subjectNormalized,
                caseId = conversation.caseId,
                status = conversation.status,
                startedAt = conversation.startedAt,
                lastMessageAt = conversation.lastMessageAt,
                createdAt = conversation.createdAt
            )
    }
}

data class MessageResponse(
    val id: UUID,
    val conversationId: UUID,
    val direction: MessageDirection,
    val status: MessageStatus,
    val providerMessageId: String?,
    val internetMessageId: String?,
    val inReplyTo: String?,
    val subject: String?,
    val fromEmail: String,
    val fromName: String?,
    val replyToEmail: String?,
    val textBody: String?,
    val htmlBody: String?,
    val sentAt: Instant?,
    val receivedAt: Instant?,
    val createdAt: Instant,
    val recipients: List<RecipientResponse> = emptyList()
) {
    companion object {
        fun from(
            message: Message,
            recipients: List<MessageRecipient> = emptyList()
        ) = MessageResponse(
            id = message.id,
            conversationId = message.conversation.id,
            direction = message.direction,
            status = message.status,
            providerMessageId = message.providerMessageId,
            internetMessageId = message.internetMessageId,
            inReplyTo = message.inReplyTo,
            subject = message.subject,
            fromEmail = message.fromEmail,
            fromName = message.fromName,
            replyToEmail = message.replyToEmail,
            textBody = message.textBody,
            htmlBody = message.htmlBody,
            sentAt = message.sentAt,
            receivedAt = message.receivedAt,
            createdAt = message.createdAt,
            recipients = recipients.map { RecipientResponse.from(it) }
        )
    }
}

data class RecipientResponse(
    val type: RecipientType,
    val email: String,
    val name: String?
) {
    companion object {
        fun from(recipient: MessageRecipient) =
            RecipientResponse(
                type = recipient.type,
                email = recipient.email,
                name = recipient.name
            )
    }
}

data class AttachmentResponse(
    val id: UUID,
    val messageId: UUID,
    val fileName: String,
    val contentType: String,
    val sizeBytes: Long?,
    val isInline: Boolean,
    val contentId: String?
) {
    companion object {
        fun from(attachment: MessageAttachment) =
            AttachmentResponse(
                id = attachment.id,
                messageId = attachment.message.id,
                fileName = attachment.fileName,
                contentType = attachment.contentType,
                sizeBytes = attachment.sizeBytes,
                isInline = attachment.isInline,
                contentId = attachment.contentId
            )
    }
}

data class MessageCreatedResponse(
    val messageId: UUID,
    val conversationId: UUID,
    val status: MessageStatus
)
