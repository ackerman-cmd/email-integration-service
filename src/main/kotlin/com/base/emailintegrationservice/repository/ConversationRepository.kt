package com.base.emailintegrationservice.repository

import com.base.emailintegrationservice.domain.entity.Conversation
import com.base.emailintegrationservice.domain.enums.ConversationStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface ConversationRepository : JpaRepository<Conversation, UUID> {
    fun findByMailboxId(mailboxId: UUID): List<Conversation>

    fun findByCaseId(caseId: UUID): Conversation?

    fun findByMailboxIdAndStatus(
        mailboxId: UUID,
        status: ConversationStatus
    ): List<Conversation>

    /**
     * Finds a conversation that contains a message with the given internet_message_id.
     * Used for threading when matching In-Reply-To / References headers.
     */
    @Query(
        """
        SELECT c FROM Conversation c
        JOIN Message m ON m.conversation = c
        WHERE m.internetMessageId = :internetMessageId
        """
    )
    fun findByMessageInternetMessageId(internetMessageId: String): Conversation?

    /**
     * Subject-fallback threading: most recent OPEN conversation in the mailbox
     * whose normalised subject matches the incoming email (after stripping Re:/Fwd: prefixes).
     * Used when the client's mail client does not send In-Reply-To / References headers.
     */
    @Query(
        """
        SELECT c FROM Conversation c
        WHERE c.mailbox.id = :mailboxId
          AND c.subjectNormalized = :subjectNormalized
          AND c.status = 'OPEN'
        ORDER BY c.lastMessageAt DESC
        """
    )
    fun findMostRecentOpenByMailboxAndSubject(
        mailboxId: java.util.UUID,
        subjectNormalized: String,
    ): Conversation?
}
