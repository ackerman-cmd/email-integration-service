package com.base.emailintegrationservice.repository

import com.base.emailintegrationservice.domain.entity.Message
import com.base.emailintegrationservice.domain.enums.MessageDirection
import com.base.emailintegrationservice.domain.enums.MessageStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface MessageRepository : JpaRepository<Message, UUID> {
    fun findByConversationIdOrderByCreatedAtAsc(conversationId: UUID): List<Message>

    fun findByProviderMessageId(providerMessageId: String): Message?

    fun findByInternetMessageId(internetMessageId: String): Message?

    fun findByConversationIdAndDirection(
        conversationId: UUID,
        direction: MessageDirection
    ): List<Message>

    fun findByStatus(status: MessageStatus): List<Message>

    /**
     * Most recent message with a known internet_message_id — used to build In-Reply-To header.
     * Spring Data Top-1 derived query is HQL-safe and portable across Hibernate versions.
     */
    fun findFirstByConversationIdAndInternetMessageIdNotNullOrderByCreatedAtDesc(conversationId: UUID): Message?

    /**
     * All internet_message_ids in a conversation ordered chronologically — for building References header.
     */
    @Query(
        """
        SELECT m.internetMessageId FROM Message m
        WHERE m.conversation.id = :conversationId
          AND m.internetMessageId IS NOT NULL
        ORDER BY m.createdAt ASC
        """
    )
    fun findAllInternetMessageIdsByConversationId(conversationId: UUID): List<String>
}
