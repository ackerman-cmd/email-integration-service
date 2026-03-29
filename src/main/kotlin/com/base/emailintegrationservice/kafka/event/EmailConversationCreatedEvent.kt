package com.base.emailintegrationservice.kafka.event

import java.time.Instant
import java.util.UUID

data class EmailConversationCreatedEvent(
    val conversationId: UUID,
    val mailboxId: UUID,
    val firstMessageId: UUID,
    val fromEmail: String,
    val subjectNormalized: String?,
    val createdAt: Instant
)
