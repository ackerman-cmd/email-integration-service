package com.base.emailintegrationservice.kafka.event

import java.time.Instant
import java.util.UUID

data class EmailInboundPersistedEvent(
    val messageId: UUID,
    val conversationId: UUID,
    val mailboxId: UUID,
    val caseId: UUID?,
    val fromEmail: String,
    val subject: String?,
    val internetMessageId: String?,
    val isNewConversation: Boolean,
    val receivedAt: Instant
)
