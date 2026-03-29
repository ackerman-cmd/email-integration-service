package com.base.emailintegrationservice.domain.entity

import com.base.emailintegrationservice.domain.enums.EmailProvider
import com.base.emailintegrationservice.domain.enums.MessageDirection
import com.base.emailintegrationservice.domain.enums.MessageStatus
import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.annotations.UpdateTimestamp
import org.hibernate.type.SqlTypes
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "message", schema = "email_service")
class Message(
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    val id: UUID = UUID.randomUUID(),
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    val conversation: Conversation,
    @Enumerated(EnumType.STRING)
    @Column(name = "direction", nullable = false)
    val direction: MessageDirection,
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    var status: MessageStatus,
    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false)
    val provider: EmailProvider = EmailProvider.RESEND,
    @Column(name = "provider_message_id")
    var providerMessageId: String? = null,
    @Column(name = "internet_message_id")
    var internetMessageId: String? = null,
    @Column(name = "in_reply_to")
    val inReplyTo: String? = null,
    @Column(name = "references_raw", columnDefinition = "TEXT")
    val referencesRaw: String? = null,
    @Column(name = "subject")
    val subject: String? = null,
    @Column(name = "from_email", nullable = false)
    val fromEmail: String,
    @Column(name = "from_name")
    val fromName: String? = null,
    @Column(name = "reply_to_email")
    val replyToEmail: String? = null,
    @Column(name = "text_body", columnDefinition = "TEXT")
    var textBody: String? = null,
    @Column(name = "html_body", columnDefinition = "TEXT")
    var htmlBody: String? = null,
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_headers_json", columnDefinition = "JSONB")
    var rawHeadersJson: String? = null,
    @Column(name = "sent_at")
    var sentAt: Instant? = null,
    @Column(name = "received_at")
    val receivedAt: Instant? = null,
    @Column(name = "created_by_user_id")
    val createdByUserId: UUID? = null,
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
)
