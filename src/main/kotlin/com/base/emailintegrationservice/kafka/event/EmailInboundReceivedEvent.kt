package com.base.emailintegrationservice.kafka.event

import java.time.Instant
import java.util.UUID

data class EmailInboundReceivedEvent(
    val providerEventId: UUID,
    val providerEmailId: String,
    val fromEmail: String,
    val toEmails: List<String>,
    val subject: String?,
    val mailboxAddress: String,
    val receivedAt: Instant
)
