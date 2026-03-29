package com.base.emailintegrationservice.kafka.event

import java.time.Instant
import java.util.UUID

data class EmailOutboundSentEvent(
    val messageId: UUID,
    val conversationId: UUID,
    val caseId: UUID?,
    val providerMessageId: String,
    val toEmails: List<String>,
    val subject: String?,
    val sentAt: Instant
)
