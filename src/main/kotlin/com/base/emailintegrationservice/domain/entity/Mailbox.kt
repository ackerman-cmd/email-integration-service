package com.base.emailintegrationservice.domain.entity

import com.base.emailintegrationservice.domain.enums.EmailProvider
import com.base.emailintegrationservice.domain.enums.MailboxStatus
import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "mailbox", schema = "email_service")
class Mailbox(
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    val id: UUID = UUID.randomUUID(),
    @Column(name = "email_address", nullable = false, unique = true)
    val emailAddress: String,
    @Column(name = "domain", nullable = false)
    val domain: String,
    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false)
    val provider: EmailProvider = EmailProvider.RESEND,
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    var status: MailboxStatus = MailboxStatus.ACTIVE,
    @Column(name = "is_inbound_enabled", nullable = false)
    var isInboundEnabled: Boolean = true,
    @Column(name = "is_outbound_enabled", nullable = false)
    var isOutboundEnabled: Boolean = true,
    @Column(name = "default_queue_key")
    val defaultQueueKey: String? = null,
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
)
