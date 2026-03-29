package com.base.emailintegrationservice.repository

import com.base.emailintegrationservice.domain.entity.MessageRecipient
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface MessageRecipientRepository : JpaRepository<MessageRecipient, UUID> {
    fun findByMessageId(messageId: UUID): List<MessageRecipient>

    fun deleteByMessageId(messageId: UUID)
}
