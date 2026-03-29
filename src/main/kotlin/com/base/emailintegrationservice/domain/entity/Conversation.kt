package com.base.emailintegrationservice.domain.entity

import com.base.emailintegrationservice.domain.enums.ConversationStatus
import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "conversation", schema = "email_service")
class Conversation(
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    val id: UUID = UUID.randomUUID(),
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mailbox_id", nullable = false)
    val mailbox: Mailbox,
    @Column(name = "subject_normalized")
    val subjectNormalized: String? = null,
    @Column(name = "case_id")
    var caseId: UUID? = null,
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    var status: ConversationStatus = ConversationStatus.OPEN,
    @Column(name = "started_at")
    val startedAt: Instant? = null,
    @Column(name = "last_message_at")
    var lastMessageAt: Instant? = null,
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
)
