package com.base.emailintegrationservice.domain.entity

import com.base.emailintegrationservice.domain.enums.EmailProvider
import com.base.emailintegrationservice.domain.enums.ProviderEventStatus
import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "provider_event", schema = "email_service")
class ProviderEvent(
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    val id: UUID = UUID.randomUUID(),
    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false)
    val provider: EmailProvider = EmailProvider.RESEND,
    @Column(name = "event_type", nullable = false)
    val eventType: String,
    @Column(name = "provider_event_id")
    val providerEventId: String? = null,
    @Column(name = "provider_message_id")
    val providerMessageId: String? = null,
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload_json", nullable = false, columnDefinition = "JSONB")
    val payloadJson: String,
    @Column(name = "deduplication_key", nullable = false, unique = true)
    val deduplicationKey: String,
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    var status: ProviderEventStatus = ProviderEventStatus.RECEIVED,
    @Column(name = "error_text", columnDefinition = "TEXT")
    var errorText: String? = null,
    @Column(name = "received_at", nullable = false)
    val receivedAt: Instant = Instant.now(),
    @Column(name = "processed_at")
    var processedAt: Instant? = null,
)
