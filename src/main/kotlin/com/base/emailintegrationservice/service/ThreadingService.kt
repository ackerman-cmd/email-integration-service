package com.base.emailintegrationservice.service

import com.base.emailintegrationservice.domain.entity.Conversation
import com.base.emailintegrationservice.repository.ConversationRepository
import com.base.emailintegrationservice.repository.MessageRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * Resolves which conversation an incoming email belongs to by matching RFC 2822 threading headers.
 *
 * Matching priority:
 * 1. In-Reply-To  → find message with that internet_message_id
 * 2. References   → find message with any of the listed IDs
 * 3. Exact internet_message_id match (duplicate / late delivery)
 * 4. Subject fallback → most recent open conversation in same mailbox with same normalised subject.
 *    Used when the client's email client strips RFC threading headers (common on mobile).
 */
@Service
class ThreadingService(
    private val conversationRepository: ConversationRepository,
    private val messageRepository: MessageRepository
) {
    private val log = LoggerFactory.getLogger(ThreadingService::class.java)

    data class ThreadingResult(
        val conversation: Conversation?,
        val matchedBy: String?
    )

    fun resolveConversation(
        inReplyTo: String?,
        referencesRaw: String?,
        internetMessageId: String?,
        mailboxId: java.util.UUID? = null,
        incomingSubject: String? = null,
    ): ThreadingResult {
        // Priority 1: In-Reply-To header
        if (!inReplyTo.isNullOrBlank()) {
            val normalized = normalizeMessageId(inReplyTo)
            val conversation = conversationRepository.findByMessageInternetMessageId(normalized)
            if (conversation != null) {
                log.info("Threaded via In-Reply-To: {}", normalized)
                return ThreadingResult(conversation, "in_reply_to")
            }
        }

        // Priority 2: References header (check each ID, ordered by occurrence)
        if (!referencesRaw.isNullOrBlank()) {
            val refIds = parseReferences(referencesRaw)
            for (refId in refIds.reversed()) {
                val normalized = normalizeMessageId(refId)
                val conversation = conversationRepository.findByMessageInternetMessageId(normalized)
                if (conversation != null) {
                    log.info("Threaded via References: {}", normalized)
                    return ThreadingResult(conversation, "references")
                }
            }
        }

        // Priority 3: Exact Message-ID match (duplicate detection / late delivery)
        if (!internetMessageId.isNullOrBlank()) {
            val normalized = normalizeMessageId(internetMessageId)
            val existing = messageRepository.findByInternetMessageId(normalized)
            if (existing != null) {
                log.info("Matched existing message by internet_message_id: {}", normalized)
                return ThreadingResult(existing.conversation, "message_id_exact")
            }
        }

        // Priority 4: Subject fallback — for clients that strip RFC threading headers.
        // Matches the most recent OPEN conversation in the same mailbox with the same normalised subject.
        if (mailboxId != null && !incomingSubject.isNullOrBlank()) {
            val normalized = normalizeSubject(incomingSubject)
            if (!normalized.isNullOrBlank()) {
                val conversation = conversationRepository.findMostRecentOpenByMailboxAndSubject(mailboxId, normalized)
                if (conversation != null) {
                    log.info("Threaded via subject fallback: mailboxId={} subject=\"{}\"", mailboxId, normalized)
                    return ThreadingResult(conversation, "subject_fallback")
                }
            }
        }

        return ThreadingResult(null, null)
    }

    /** Strips Re:/Fwd: prefixes repeatedly until the core subject remains. */
    fun normalizeSubject(subject: String?): String? {
        if (subject.isNullOrBlank()) return null
        val prefixRegex = Regex("^(Re|Fwd|Fw|ОТ|От)\\s*(?:\\[\\d+])?\\s*:\\s*", RegexOption.IGNORE_CASE)
        var result = subject.trim()
        var prev: String
        do {
            prev = result
            result = prefixRegex.replace(result, "").trim()
        } while (result != prev)
        return result.ifBlank { null }
    }

    fun buildReferencesHeader(
        conversationId: java.util.UUID,
        newInReplyTo: String?
    ): String {
        val existing = messageRepository.findAllInternetMessageIdsByConversationId(conversationId)
        return (existing + listOfNotNull(newInReplyTo)).distinct().joinToString(" ")
    }

    private fun parseReferences(raw: String): List<String> = raw.split(Regex("\\s+")).filter { it.isNotBlank() }

    /** Strips surrounding angle brackets if present. */
    private fun normalizeMessageId(id: String): String =
        id.trim().let { if (it.startsWith("<") && it.endsWith(">")) it.substring(1, it.length - 1) else it }
}
