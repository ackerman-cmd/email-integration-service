package com.base.emailintegrationservice.domain.entity

import jakarta.persistence.*
import java.util.UUID

@Entity
@Table(name = "message_attachment", schema = "email_service")
class MessageAttachment(
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    val id: UUID = UUID.randomUUID(),
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id", nullable = false)
    val message: Message,
    @Column(name = "provider_attachment_id")
    val providerAttachmentId: String? = null,
    @Column(name = "file_name", nullable = false)
    val fileName: String,
    @Column(name = "content_type", nullable = false)
    val contentType: String,
    @Column(name = "size_bytes")
    val sizeBytes: Long? = null,
    @Column(name = "storage_key")
    var storageKey: String? = null,
    @Column(name = "checksum")
    var checksum: String? = null,
    @Column(name = "is_inline", nullable = false)
    val isInline: Boolean = false,
    @Column(name = "content_id")
    val contentId: String? = null,
)
