package com.base.emailintegrationservice.domain.entity

import com.base.emailintegrationservice.domain.enums.OutboxEventStatus
import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "outbox_event", schema = "email_service")
class OutboxEvent(
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    val id: UUID = UUID.randomUUID(),
    @Column(name = "aggregate_type", nullable = false)
    val aggregateType: String,
    @Column(name = "aggregate_id", nullable = false)
    val aggregateId: String,
    @Column(name = "event_type", nullable = false)
    val eventType: String,
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload_json", nullable = false, columnDefinition = "JSONB")
    val payloadJson: String,
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    var status: OutboxEventStatus = OutboxEventStatus.PENDING,
    @Column(name = "retry_count", nullable = false)
    var retryCount: Int = 0,
    @Column(name = "next_retry_at")
    var nextRetryAt: Instant? = null,
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),
)
