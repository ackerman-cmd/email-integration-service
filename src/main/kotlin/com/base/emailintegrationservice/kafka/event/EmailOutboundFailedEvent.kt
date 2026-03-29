package com.base.emailintegrationservice.kafka.event

import java.time.Instant
import java.util.UUID

data class EmailOutboundFailedEvent(
    val messageId: UUID,
    val conversationId: UUID,
    val caseId: UUID?,
    val reason: String,
    val failedAt: Instant
)
