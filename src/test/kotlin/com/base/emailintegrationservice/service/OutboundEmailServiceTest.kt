package com.base.emailintegrationservice.service

import com.base.emailintegrationservice.domain.entity.Conversation
import com.base.emailintegrationservice.domain.entity.Mailbox
import com.base.emailintegrationservice.domain.entity.Message
import com.base.emailintegrationservice.domain.entity.OutboxEvent
import com.base.emailintegrationservice.domain.enums.MessageDirection
import com.base.emailintegrationservice.domain.enums.MessageStatus
import com.base.emailintegrationservice.integration.resend.ResendApiException
import com.base.emailintegrationservice.integration.resend.ResendClient
import com.base.emailintegrationservice.integration.resend.dto.ResendSendRequest
import com.base.emailintegrationservice.integration.resend.dto.ResendSendResponse
import com.base.emailintegrationservice.kafka.KafkaEventPublisher
import com.base.emailintegrationservice.repository.ConversationRepository
import com.base.emailintegrationservice.repository.MailboxRepository
import com.base.emailintegrationservice.repository.MessageRecipientRepository
import com.base.emailintegrationservice.repository.MessageRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.Optional

class OutboundEmailServiceTest {
    private val mailboxRepository: MailboxRepository = mockk()
    private val conversationRepository: ConversationRepository = mockk()
    private val messageRepository: MessageRepository = mockk()
    private val messageRecipientRepository: MessageRecipientRepository = mockk()
    private val threadingService: ThreadingService = mockk()
    private val resendClient: ResendClient = mockk()
    private val kafkaEventPublisher: KafkaEventPublisher = mockk()

    private lateinit var outboundEmailService: OutboundEmailService

    private lateinit var mailbox: Mailbox
    private lateinit var conversation: Conversation
    private lateinit var savedMessage: Message

    @BeforeEach
    fun setUp() {
        outboundEmailService = OutboundEmailService(
            mailboxRepository,
            conversationRepository,
            messageRepository,
            messageRecipientRepository,
            threadingService,
            resendClient,
            kafkaEventPublisher,
        )

        mailbox = Mailbox(emailAddress = "support@test.com", domain = "test.com", isOutboundEnabled = true)
        conversation = Conversation(mailbox = mailbox)
        savedMessage = Message(
            conversation = conversation,
            direction = MessageDirection.OUTBOUND,
            status = MessageStatus.PENDING_SEND,
            fromEmail = "support@test.com",
            subject = "Test subject",
        )

        every { conversationRepository.save(any()) } answers { firstArg() }
        every { messageRepository.save(any()) } returns savedMessage
        every { messageRecipientRepository.save(any()) } answers { firstArg() }
        every { kafkaEventPublisher.saveToOutbox(any(), any(), any(), any()) } returns mockk<OutboxEvent>()
    }

    // ── sendNew ────────────────────────────────────────────────────────────

    @Test
    fun `sendNew creates conversation and sends email via Resend`() {
        every { mailboxRepository.findById(mailbox.id) } returns Optional.of(mailbox)
        every { resendClient.sendEmail(any(), any()) } returns ResendSendResponse("em_123")

        outboundEmailService.sendNew(
            mailboxId = mailbox.id,
            to = listOf("user@example.com"),
            subject = "Hello",
            htmlBody = "<p>Hi</p>",
            textBody = "Hi",
            createdByUserId = null,
        )

        verify { conversationRepository.save(any()) }
        verify { resendClient.sendEmail(any(), any()) }
    }

    @Test
    fun `sendNew uses message UUID as idempotency key`() {
        every { mailboxRepository.findById(mailbox.id) } returns Optional.of(mailbox)
        val idempotencySlot = slot<String>()
        every { resendClient.sendEmail(any(), capture(idempotencySlot)) } returns ResendSendResponse("em_123")

        outboundEmailService.sendNew(
            mailboxId = mailbox.id,
            to = listOf("user@example.com"),
            subject = "Hello",
            htmlBody = null,
            textBody = "Hi",
            createdByUserId = null,
        )

        assertThat(idempotencySlot.captured).isEqualTo(savedMessage.id.toString())
    }

    @Test
    fun `sendNew sets message status to FAILED when Resend throws`() {
        every { mailboxRepository.findById(mailbox.id) } returns Optional.of(mailbox)
        every { resendClient.sendEmail(any(), any()) } throws ResendApiException("timeout")

        outboundEmailService.sendNew(
            mailboxId = mailbox.id,
            to = listOf("user@example.com"),
            subject = "Hello",
            htmlBody = null,
            textBody = "Hi",
            createdByUserId = null,
        )

        assertThat(savedMessage.status).isEqualTo(MessageStatus.FAILED)
        verify(atLeast = 2) { messageRepository.save(any()) }
    }

    @Test
    fun `sendNew publishes OUTBOUND_REQUESTED then OUTBOUND_SENT on success`() {
        every { mailboxRepository.findById(mailbox.id) } returns Optional.of(mailbox)
        every { resendClient.sendEmail(any(), any()) } returns ResendSendResponse("em_abc")

        outboundEmailService.sendNew(
            mailboxId = mailbox.id,
            to = listOf("user@example.com"),
            subject = "Hello",
            htmlBody = null,
            textBody = "Hi",
            createdByUserId = null,
        )

        val topics = mutableListOf<String>()
        verify(exactly = 2) {
            kafkaEventPublisher.saveToOutbox(capture(topics), any(), any(), any())
        }
        assertThat(topics).containsExactly("email.outbound.requested", "email.outbound.sent")
    }

    @Test
    fun `sendNew publishes OUTBOUND_FAILED when Resend throws`() {
        every { mailboxRepository.findById(mailbox.id) } returns Optional.of(mailbox)
        every { resendClient.sendEmail(any(), any()) } throws ResendApiException("error")

        outboundEmailService.sendNew(
            mailboxId = mailbox.id,
            to = listOf("user@example.com"),
            subject = "Hello",
            htmlBody = null,
            textBody = "Hi",
            createdByUserId = null,
        )

        val topics = mutableListOf<String>()
        verify(exactly = 2) {
            kafkaEventPublisher.saveToOutbox(capture(topics), any(), any(), any())
        }
        assertThat(topics).containsExactly("email.outbound.requested", "email.outbound.failed")
    }

    // ── reply ──────────────────────────────────────────────────────────────

    @Test
    fun `reply adds In-Reply-To header pointing to previous message`() {
        val previousMessage = Message(
            conversation = conversation,
            direction = MessageDirection.INBOUND,
            status = MessageStatus.RECEIVED_FULL,
            fromEmail = "user@example.com",
            internetMessageId = "prev-msg@user.com",
        )
        every { conversationRepository.findById(conversation.id) } returns Optional.of(conversation)
        every { messageRepository.findById(any()) } returns Optional.empty()
        every {
            messageRepository.findFirstByConversationIdAndInternetMessageIdNotNullOrderByCreatedAtDesc(conversation.id)
        } returns previousMessage
        every { threadingService.buildReferencesHeader(any(), any()) } returns "prev-msg@user.com"

        val requestSlot = slot<ResendSendRequest>()
        every { resendClient.sendEmail(capture(requestSlot), any()) } returns ResendSendResponse("em_reply")

        outboundEmailService.reply(
            conversationId = conversation.id,
            replyToMessageId = null,
            to = listOf("user@example.com"),
            htmlBody = "<p>Reply</p>",
            textBody = "Reply",
            createdByUserId = null,
        )

        assertThat(requestSlot.captured.headers).containsKey("In-Reply-To")
        assertThat(requestSlot.captured.headers!!["In-Reply-To"]).contains("prev-msg@user.com")
    }

    @Test
    fun `reply prefixes subject with Re`() {
        val previousMessage = Message(
            conversation = conversation,
            direction = MessageDirection.INBOUND,
            status = MessageStatus.RECEIVED_FULL,
            fromEmail = "user@example.com",
            subject = "My issue",
            internetMessageId = "prev@user.com",
        )
        every { conversationRepository.findById(conversation.id) } returns Optional.of(conversation)
        every { messageRepository.findById(any()) } returns Optional.empty()
        every {
            messageRepository.findFirstByConversationIdAndInternetMessageIdNotNullOrderByCreatedAtDesc(conversation.id)
        } returns previousMessage
        every { threadingService.buildReferencesHeader(any(), any()) } returns "prev@user.com"

        val requestSlot = slot<ResendSendRequest>()
        every { resendClient.sendEmail(capture(requestSlot), any()) } returns ResendSendResponse("em_re")

        outboundEmailService.reply(
            conversationId = conversation.id,
            replyToMessageId = null,
            to = listOf("user@example.com"),
            htmlBody = null,
            textBody = "ok",
            createdByUserId = null,
        )

        assertThat(requestSlot.captured.subject).startsWith("Re:")
    }

    // ── forward ────────────────────────────────────────────────────────────

    @Test
    fun `forward includes original sender and note in body`() {
        val originalMessage = Message(
            conversation = conversation,
            direction = MessageDirection.INBOUND,
            status = MessageStatus.RECEIVED_FULL,
            fromEmail = "original-sender@example.com",
            subject = "Original subject",
            textBody = "Original body",
        )
        every { messageRepository.findById(originalMessage.id) } returns Optional.of(originalMessage)
        every { messageRepository.save(any()) } answers { firstArg() }

        val requestSlot = slot<ResendSendRequest>()
        every { resendClient.sendEmail(capture(requestSlot), any()) } returns ResendSendResponse("em_fwd")

        outboundEmailService.forward(
            messageId = originalMessage.id,
            to = listOf("third-party@example.com"),
            note = "Please check this",
            createdByUserId = null,
        )

        assertThat(requestSlot.captured.subject).startsWith("Fwd:")
        assertThat(requestSlot.captured.text).contains("original-sender@example.com")
        assertThat(requestSlot.captured.text).contains("Please check this")
    }

    @Test
    fun `forward sets FAILED status when Resend throws`() {
        val originalMessage = Message(
            conversation = conversation,
            direction = MessageDirection.INBOUND,
            status = MessageStatus.RECEIVED_FULL,
            fromEmail = "sender@example.com",
            subject = "Subject",
        )
        val capturedMessages = mutableListOf<Message>()
        every { messageRepository.findById(originalMessage.id) } returns Optional.of(originalMessage)
        every { messageRepository.save(capture(capturedMessages)) } answers { firstArg() }
        every { resendClient.sendEmail(any(), any()) } throws ResendApiException("network error")

        outboundEmailService.forward(
            messageId = originalMessage.id,
            to = listOf("fwd@example.com"),
            note = null,
            createdByUserId = null,
        )

        assertThat(capturedMessages.last().status).isEqualTo(MessageStatus.FAILED)
    }
}
