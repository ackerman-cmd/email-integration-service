package com.base.emailintegrationservice.domain.entity

import com.base.emailintegrationservice.domain.enums.RecipientType
import jakarta.persistence.*
import java.util.UUID

@Entity
@Table(name = "message_recipient", schema = "email_service")
class MessageRecipient(
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    val id: UUID = UUID.randomUUID(),
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id", nullable = false)
    val message: Message,
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    val type: RecipientType,
    @Column(name = "email", nullable = false)
    val email: String,
    @Column(name = "name")
    val name: String? = null,
)
