package com.base.emailintegrationservice.kafka.event

import java.time.Instant
import java.util.UUID

data class EmailOutboundRequestedEvent(
    val messageId: UUID,
    val conversationId: UUID,
    val createdByUserId: UUID?,
    val toEmails: List<String>,
    val subject: String?,
    val requestedAt: Instant
)
