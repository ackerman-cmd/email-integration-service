package com.base.emailintegrationservice.service

import com.base.emailintegrationservice.domain.entity.Conversation
import com.base.emailintegrationservice.domain.entity.Mailbox
import com.base.emailintegrationservice.domain.entity.Message
import com.base.emailintegrationservice.domain.enums.MessageDirection
import com.base.emailintegrationservice.domain.enums.MessageStatus
import com.base.emailintegrationservice.repository.ConversationRepository
import com.base.emailintegrationservice.repository.MessageRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

class ThreadingServiceTest {
    private val conversationRepository: ConversationRepository = mockk()
    private val messageRepository: MessageRepository = mockk()

    private lateinit var threadingService: ThreadingService

    private lateinit var mailbox: Mailbox
    private lateinit var conversation: Conversation
    private lateinit var message: Message

    @BeforeEach
    fun setUp() {
        threadingService = ThreadingService(conversationRepository, messageRepository)
        mailbox = Mailbox(emailAddress = "support@test.com", domain = "test.com")
        conversation = Conversation(mailbox = mailbox)
        message = Message(
            conversation = conversation,
            direction = MessageDirection.OUTBOUND,
            status = MessageStatus.SENT,
            fromEmail = "support@test.com",
            internetMessageId = "original-msg-id@test.com",
        )
    }

    @Test
    fun `resolves conversation by In-Reply-To header`() {
        every {
            conversationRepository.findByMessageInternetMessageId("original-msg-id@test.com")
        } returns conversation

        val result = threadingService.resolveConversation(
            inReplyTo = "<original-msg-id@test.com>",
            referencesRaw = null,
            internetMessageId = null,
        )

        assertThat(result.conversation).isEqualTo(conversation)
        assertThat(result.matchedBy).isEqualTo("in_reply_to")
        verify(exactly = 1) { conversationRepository.findByMessageInternetMessageId("original-msg-id@test.com") }
    }

    @Test
    fun `resolves conversation by References header when In-Reply-To not found`() {
        every {
            conversationRepository.findByMessageInternetMessageId("original-msg-id@test.com")
        } returns null andThen conversation

        val result = threadingService.resolveConversation(
            inReplyTo = "<original-msg-id@test.com>",
            referencesRaw = "<older-id@test.com> <original-msg-id@test.com>",
            internetMessageId = null,
        )

        assertThat(result.conversation).isEqualTo(conversation)
        assertThat(result.matchedBy).isEqualTo("references")
    }

    @Test
    fun `In-Reply-To takes priority over References`() {
        every { conversationRepository.findByMessageInternetMessageId("reply-to-id@test.com") } returns conversation

        val result = threadingService.resolveConversation(
            inReplyTo = "<reply-to-id@test.com>",
            referencesRaw = "<other-id@test.com>",
            internetMessageId = null,
        )

        assertThat(result.conversation).isEqualTo(conversation)
        assertThat(result.matchedBy).isEqualTo("in_reply_to")
        // References should not be queried when In-Reply-To already matched
        verify(exactly = 0) { conversationRepository.findByMessageInternetMessageId("other-id@test.com") }
    }

    @Test
    fun `falls back to exact message_id match when headers not found in DB`() {
        every { conversationRepository.findByMessageInternetMessageId(any()) } returns null
        every { messageRepository.findByInternetMessageId("exact-id@test.com") } returns message

        val result = threadingService.resolveConversation(
            inReplyTo = null,
            referencesRaw = null,
            internetMessageId = "<exact-id@test.com>",
        )

        assertThat(result.conversation).isEqualTo(conversation)
        assertThat(result.matchedBy).isEqualTo("message_id_exact")
    }

    @Test
    fun `returns null conversation when no headers match anything`() {
        every { conversationRepository.findByMessageInternetMessageId(any()) } returns null
        every { messageRepository.findByInternetMessageId(any()) } returns null

        val result = threadingService.resolveConversation(
            inReplyTo = "<unknown@test.com>",
            referencesRaw = null,
            internetMessageId = "<also-unknown@test.com>",
        )

        assertThat(result.conversation).isNull()
        assertThat(result.matchedBy).isNull()
    }

    @Test
    fun `normalizes angle brackets and whitespace when looking up message IDs`() {
        every { conversationRepository.findByMessageInternetMessageId("clean-id@test.com") } returns conversation

        val result = threadingService.resolveConversation(
            inReplyTo = "  <clean-id@test.com>  ",
            referencesRaw = null,
            internetMessageId = null,
        )

        assertThat(result.conversation).isEqualTo(conversation)
        verify { conversationRepository.findByMessageInternetMessageId("clean-id@test.com") }
    }

    @Test
    fun `buildReferencesHeader combines existing IDs and deduplicates`() {
        val convId = UUID.randomUUID()
        every {
            messageRepository.findAllInternetMessageIdsByConversationId(convId)
        } returns listOf("first@test.com", "second@test.com")

        val result = threadingService.buildReferencesHeader(convId, "second@test.com")

        // second@test.com appears once (deduplicated)
        assertThat(result).isEqualTo("first@test.com second@test.com")
    }

    @Test
    fun `buildReferencesHeader works with empty conversation history`() {
        val convId = UUID.randomUUID()
        every { messageRepository.findAllInternetMessageIdsByConversationId(convId) } returns emptyList()

        val result = threadingService.buildReferencesHeader(convId, "only-msg@test.com")

        assertThat(result).isEqualTo("only-msg@test.com")
    }
}
