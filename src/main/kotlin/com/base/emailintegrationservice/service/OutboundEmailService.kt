package com.base.emailintegrationservice.service

import com.base.emailintegrationservice.config.KafkaTopics
import com.base.emailintegrationservice.domain.entity.*
import com.base.emailintegrationservice.domain.enums.*
import com.base.emailintegrationservice.integration.resend.ResendClient
import com.base.emailintegrationservice.integration.resend.dto.ResendAttachmentRequest
import com.base.emailintegrationservice.integration.resend.dto.ResendSendRequest
import com.base.emailintegrationservice.kafka.KafkaEventPublisher
import com.base.emailintegrationservice.kafka.event.EmailOutboundFailedEvent
import com.base.emailintegrationservice.kafka.event.EmailOutboundRequestedEvent
import com.base.emailintegrationservice.kafka.event.EmailOutboundSentEvent
import com.base.emailintegrationservice.repository.*
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.time.Instant
import java.util.Base64
import java.util.UUID

@Service
class OutboundEmailService(
    private val mailboxRepository: MailboxRepository,
    private val conversationRepository: ConversationRepository,
    private val messageRepository: MessageRepository,
    private val messageRecipientRepository: MessageRecipientRepository,
    private val messageAttachmentRepository: MessageAttachmentRepository,
    private val threadingService: ThreadingService,
    private val resendClient: ResendClient,
    private val s3StorageService: S3StorageService,
    private val kafkaEventPublisher: KafkaEventPublisher,
) {
    private val log = LoggerFactory.getLogger(OutboundEmailService::class.java)

    @Transactional
    fun sendNew(
        fromEmail: String,
        to: List<String>,
        cc: List<String> = emptyList(),
        subject: String,
        htmlBody: String?,
        textBody: String?,
        createdByUserId: UUID?,
        files: List<MultipartFile> = emptyList(),
    ): Message {
        val mailbox = resolveOutboundMailboxByEmail(fromEmail)

        val conversation = conversationRepository.save(
            Conversation(
                mailbox = mailbox,
                subjectNormalized = threadingService.normalizeSubject(subject),
                startedAt = Instant.now(),
                lastMessageAt = Instant.now(),
            ),
        )

        val message = createOutboundMessage(
            conversation = conversation,
            fromEmail = mailbox.emailAddress,
            to = to,
            cc = cc,
            subject = subject,
            htmlBody = htmlBody,
            textBody = textBody,
            createdByUserId = createdByUserId,
        )

        val attachments = uploadAttachments(message, files)

        kafkaEventPublisher.saveToOutbox(
            topic = KafkaTopics.EMAIL_OUTBOUND_REQUESTED,
            aggregateType = "message",
            aggregateId = message.id.toString(),
            payload = EmailOutboundRequestedEvent(
                messageId = message.id,
                conversationId = conversation.id,
                createdByUserId = createdByUserId,
                toEmails = to,
                subject = subject,
                requestedAt = Instant.now(),
            ),
        )

        sendViaResend(message, conversation, mailbox, to, cc, subject, null, null, attachments)
        return message
    }

    @Transactional
    fun reply(
        conversationId: UUID,
        replyToMessageId: UUID?,
        to: List<String>,
        cc: List<String> = emptyList(),
        htmlBody: String?,
        textBody: String?,
        createdByUserId: UUID?,
        files: List<MultipartFile> = emptyList(),
    ): Message {
        val conversation = conversationRepository
            .findById(conversationId)
            .orElseThrow { IllegalArgumentException("Conversation not found: $conversationId") }
        val mailbox = conversation.mailbox
        require(mailbox.isOutboundEnabled) { "Outbound sending is disabled for mailbox: ${mailbox.emailAddress}" }

        val replyToMessage = replyToMessageId?.let {
            messageRepository.findById(it).orElse(null)
        } ?: messageRepository.findFirstByConversationIdAndInternetMessageIdNotNullOrderByCreatedAtDesc(conversationId)

        val inReplyTo = replyToMessage?.internetMessageId
        val references = threadingService.buildReferencesHeader(conversationId, inReplyTo)
        val subject = "Re: ${replyToMessage?.subject ?: ""}"

        val message = createOutboundMessage(
            conversation = conversation,
            fromEmail = mailbox.emailAddress,
            to = to,
            cc = cc,
            subject = subject,
            htmlBody = htmlBody,
            textBody = textBody,
            createdByUserId = createdByUserId,
        )

        conversation.lastMessageAt = Instant.now()
        conversationRepository.save(conversation)

        val attachments = uploadAttachments(message, files)

        kafkaEventPublisher.saveToOutbox(
            topic = KafkaTopics.EMAIL_OUTBOUND_REQUESTED,
            aggregateType = "message",
            aggregateId = message.id.toString(),
            payload = EmailOutboundRequestedEvent(
                messageId = message.id,
                conversationId = conversation.id,
                createdByUserId = createdByUserId,
                toEmails = to,
                subject = subject,
                requestedAt = Instant.now(),
            ),
        )

        sendViaResend(message, conversation, mailbox, to, cc, subject, inReplyTo, references, attachments)
        return message
    }

    @Transactional
    fun forward(
        messageId: UUID,
        to: List<String>,
        note: String?,
        createdByUserId: UUID?,
    ): Message {
        val original = messageRepository
            .findById(messageId)
            .orElseThrow { IllegalArgumentException("Message not found: $messageId") }
        val conversation = original.conversation
        val mailbox = conversation.mailbox
        require(mailbox.isOutboundEnabled) { "Outbound sending is disabled for mailbox: ${mailbox.emailAddress}" }

        val fwdSubject = "Fwd: ${original.subject ?: ""}"

        val fwdMessage = createOutboundMessage(
            conversation = conversation,
            fromEmail = mailbox.emailAddress,
            to = to,
            cc = emptyList(),
            subject = fwdSubject,
            htmlBody = buildForwardHtml(original, note),
            textBody = buildForwardText(original, note),
            createdByUserId = createdByUserId,
        )

        conversation.lastMessageAt = Instant.now()
        conversationRepository.save(conversation)

        kafkaEventPublisher.saveToOutbox(
            topic = KafkaTopics.EMAIL_OUTBOUND_REQUESTED,
            aggregateType = "message",
            aggregateId = fwdMessage.id.toString(),
            payload = EmailOutboundRequestedEvent(
                messageId = fwdMessage.id,
                conversationId = conversation.id,
                createdByUserId = createdByUserId,
                toEmails = to,
                subject = fwdSubject,
                requestedAt = Instant.now(),
            ),
        )

        sendViaResend(fwdMessage, conversation, mailbox, to, emptyList(), fwdSubject, null, null, emptyList())
        return fwdMessage
    }

    private fun createOutboundMessage(
        conversation: Conversation,
        fromEmail: String,
        to: List<String>,
        cc: List<String>,
        subject: String,
        htmlBody: String?,
        textBody: String?,
        createdByUserId: UUID?,
    ): Message {
        val message = messageRepository.save(
            Message(
                conversation = conversation,
                direction = MessageDirection.OUTBOUND,
                status = MessageStatus.PENDING_SEND,
                fromEmail = fromEmail,
                subject = subject,
                htmlBody = htmlBody,
                textBody = textBody,
                createdByUserId = createdByUserId,
            ),
        )
        to.forEach { email ->
            messageRecipientRepository.save(MessageRecipient(message = message, type = RecipientType.TO, email = email))
        }
        cc.forEach { email ->
            messageRecipientRepository.save(MessageRecipient(message = message, type = RecipientType.CC, email = email))
        }
        return message
    }

    private fun sendViaResend(
        message: Message,
        conversation: Conversation,
        mailbox: Mailbox,
        to: List<String>,
        cc: List<String>,
        subject: String,
        inReplyTo: String?,
        references: String?,
        attachments: List<ResendAttachmentRequest> = emptyList(),
    ) {
        message.status = MessageStatus.SENDING

        // Generate a stable RFC 2822 Message-ID so ThreadingService can match client replies.
        // Format: <{message-uuid}@{mailbox-domain}>
        val domain = mailbox.emailAddress.substringAfter("@").ifBlank { "mail.local" }
        val outboundMessageId = "${message.id}@$domain"
        message.internetMessageId = outboundMessageId

        messageRepository.save(message)

        val headers = mutableMapOf<String, String>()
        headers["Message-ID"] = "<$outboundMessageId>"
        if (!inReplyTo.isNullOrBlank()) headers["In-Reply-To"] = "<$inReplyTo>"
        if (!references.isNullOrBlank()) headers["References"] = references

        val request = ResendSendRequest(
            from = mailbox.emailAddress,
            to = to,
            cc = cc.ifEmpty { null },
            subject = subject,
            html = message.htmlBody,
            text = message.textBody,
            replyTo = listOf(mailbox.emailAddress),
            headers = headers.ifEmpty { null },
            attachments = attachments.ifEmpty { null },
        )

        try {
            val response = resendClient.sendEmail(request, idempotencyKey = message.id.toString())
            message.providerMessageId = response.id
            message.status = MessageStatus.SENT
            message.sentAt = Instant.now()
            messageRepository.save(message)

            kafkaEventPublisher.saveToOutbox(
                topic = KafkaTopics.EMAIL_OUTBOUND_SENT,
                aggregateType = "message",
                aggregateId = message.id.toString(),
                payload = EmailOutboundSentEvent(
                    messageId = message.id,
                    conversationId = conversation.id,
                    caseId = conversation.caseId,
                    providerMessageId = response.id,
                    toEmails = to,
                    subject = subject,
                    sentAt = Instant.now(),
                ),
            )

            log.info("Outbound email sent: messageId={} providerMessageId={}", message.id, response.id)
        } catch (ex: Exception) {
            log.error("Failed to send email messageId={}: [{}] {}", message.id, ex::class.simpleName, ex.message, ex)
            message.status = MessageStatus.FAILED
            messageRepository.save(message)

            kafkaEventPublisher.saveToOutbox(
                topic = KafkaTopics.EMAIL_OUTBOUND_FAILED,
                aggregateType = "message",
                aggregateId = message.id.toString(),
                payload = EmailOutboundFailedEvent(
                    messageId = message.id,
                    conversationId = conversation.id,
                    caseId = conversation.caseId,
                    reason = ex.message ?: "unknown",
                    failedAt = Instant.now(),
                ),
            )
        }
    }

    private fun uploadAttachments(
        message: Message,
        files: List<MultipartFile>,
    ): List<ResendAttachmentRequest> {
        if (files.isEmpty()) return emptyList()
        return files.map { file ->
            val fileName = file.originalFilename ?: file.name
            val contentType = file.contentType ?: "application/octet-stream"
            val bytes = file.bytes
            val s3Key = "outbound/${message.conversation.id}/${message.id}/$fileName"
            val s3Url = s3StorageService.upload(s3Key, bytes, contentType)
            messageAttachmentRepository.save(
                MessageAttachment(
                    message = message,
                    fileName = fileName,
                    contentType = contentType,
                    sizeBytes = bytes.size.toLong(),
                    storageKey = s3Key,
                ),
            )
            log.debug("Uploaded email attachment to S3: key={} url={}", s3Key, s3Url)
            ResendAttachmentRequest(
                filename = fileName,
                content = Base64.getEncoder().encodeToString(bytes),
                contentType = contentType,
            )
        }
    }

    private fun resolveOutboundMailboxByEmail(raw: String): Mailbox {
        val normalized = raw.trim().lowercase()
        require(normalized.isNotEmpty()) { "From email must not be blank" }

        val mailbox =
            mailboxRepository.findByEmailAddressIgnoreCase(normalized)
                ?: throw IllegalArgumentException("Mailbox not found: $normalized")

        require(mailbox.status == MailboxStatus.ACTIVE) {
            "Mailbox is not active: ${mailbox.emailAddress}"
        }
        require(mailbox.isOutboundEnabled) {
            "Outbound sending is disabled for mailbox: ${mailbox.emailAddress}"
        }
        return mailbox
    }

    private fun buildForwardHtml(
        original: Message,
        note: String?
    ): String =
        buildString {
            if (!note.isNullOrBlank()) append("<p>$note</p><br/>")
            append("<div style=\"border-left:2px solid #ccc;padding-left:10px;\">")
            append("<p><b>From:</b> ${original.fromEmail}</p>")
            append("<p><b>Subject:</b> ${original.subject}</p>")
            append(original.htmlBody ?: original.textBody?.let { "<pre>$it</pre>" } ?: "")
            append("</div>")
        }

    private fun buildForwardText(
        original: Message,
        note: String?
    ): String =
        buildString {
            if (!note.isNullOrBlank()) appendLine(note).appendLine()
            appendLine("-------- Forwarded Message --------")
            appendLine("From: ${original.fromEmail}")
            appendLine("Subject: ${original.subject}")
            appendLine()
            append(original.textBody ?: "")
        }
}
