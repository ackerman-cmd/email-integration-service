package com.base.emailintegrationservice.kafka.event

import java.time.Instant
import java.util.UUID

data class EmailInboundFailedEvent(
    val providerEventId: UUID,
    val providerEmailId: String,
    val reason: String,
    val failedAt: Instant
)
