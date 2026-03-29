package com.base.emailintegrationservice.kafka.event

import java.time.Instant
import java.util.UUID

data class EmailConversationMatchedEvent(
    val messageId: UUID,
    val conversationId: UUID,
    val caseId: UUID?,
    val matchedBy: String,
    val matchedAt: Instant
)
