package com.base.emailintegrationservice.repository

import com.base.emailintegrationservice.domain.entity.MessageAttachment
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface MessageAttachmentRepository : JpaRepository<MessageAttachment, UUID> {
    fun findByMessageId(messageId: UUID): List<MessageAttachment>

    fun findByMessageIdIn(messageIds: List<UUID>): List<MessageAttachment>
}
