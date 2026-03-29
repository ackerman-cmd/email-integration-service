package com.base.emailintegrationservice.repository

import com.base.emailintegrationservice.domain.entity.Mailbox
import com.base.emailintegrationservice.domain.enums.MailboxStatus
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface MailboxRepository : JpaRepository<Mailbox, UUID> {
    fun findByEmailAddress(emailAddress: String): Mailbox?

    fun findByStatus(status: MailboxStatus): List<Mailbox>

    fun existsByEmailAddress(emailAddress: String): Boolean
}
