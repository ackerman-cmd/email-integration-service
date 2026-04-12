package com.base.emailintegrationservice.webhook

import com.base.emailintegrationservice.config.KafkaTopics
import com.base.emailintegrationservice.domain.entity.Conversation
import com.base.emailintegrationservice.domain.entity.Message
import com.base.emailintegrationservice.domain.entity.MessageAttachment
import com.base.emailintegrationservice.domain.entity.MessageRecipient
import com.base.emailintegrationservice.domain.entity.ProviderEvent
import com.base.emailintegrationservice.domain.enums.MessageDirection
import com.base.emailintegrationservice.domain.enums.MessageStatus
import com.base.emailintegrationservice.domain.enums.ProviderEventStatus
import com.base.emailintegrationservice.domain.enums.RecipientType
import com.base.emailintegrationservice.integration.resend.ResendClient
import com.base.emailintegrationservice.kafka.KafkaEventPublisher
import com.base.emailintegrationservice.kafka.event.EmailConversationCreatedEvent
import com.base.emailintegrationservice.kafka.event.EmailConversationMatchedEvent
import com.base.emailintegrationservice.kafka.event.EmailInboundPersistedEvent
import com.base.emailintegrationservice.kafka.event.EmailOutboundFailedEvent
import com.base.emailintegrationservice.kafka.event.EmailOutboundSentEvent
import com.base.emailintegrationservice.repository.ConversationRepository
import com.base.emailintegrationservice.repository.MailboxRepository
import com.base.emailintegrationservice.repository.MessageAttachmentRepository
import com.base.emailintegrationservice.repository.MessageRecipientRepository
import com.base.emailintegrationservice.repository.MessageRepository
import com.base.emailintegrationservice.repository.ProviderEventRepository
import com.base.emailintegrationservice.service.ThreadingService
import com.base.emailintegrationservice.webhook.dto.ResendWebhookPayload
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class ResendWebhookTransactionalProcessor(
    private val providerEventRepository: ProviderEventRepository,
    private val mailboxRepository: MailboxRepository,
    private val conversationRepository: ConversationRepository,
    private val messageRepository: MessageRepository,
    private val messageRecipientRepository: MessageRecipientRepository,
    private val messageAttachmentRepository: MessageAttachmentRepository,
    private val threadingService: ThreadingService,
    private val resendClient: ResendClient,
    private val kafkaEventPublisher: KafkaEventPublisher,
    private val objectMapper: ObjectMapper,
) {
    private val log = LoggerFactory.getLogger(ResendWebhookTransactionalProcessor::class.java)

    @Transactional
    fun processInboundEmail(
        payload: ResendWebhookPayload,
        rawBody: String
    ) {
        val data = payload.data
        val emailId = data.emailId ?: run {
            log.warn("Inbound webhook missing emailId, skipping")
            return
        }
        val dedupKey = "inbound:$emailId"

        if (providerEventRepository.existsByDeduplicationKey(dedupKey)) {
            log.info("Duplicate inbound webhook ignored: emailId={}", emailId)
            return
        }

        val providerEvent = providerEventRepository.save(
            ProviderEvent(
                eventType = payload.type,
                providerEventId = emailId,
                providerMessageId = emailId,
                payloadJson = rawBody,
                deduplicationKey = dedupKey,
                status = ProviderEventStatus.PROCESSING,
            ),
        )

        val toAddress = data.to?.firstOrNull()
        val mailbox = toAddress?.let { mailboxRepository.findByEmailAddress(it) }
        if (mailbox == null) {
            log.warn("No mailbox found for inbound email to={}", toAddress)
            providerEvent.status = ProviderEventStatus.FAILED
            providerEvent.errorText = "No mailbox configured for $toAddress"
            providerEventRepository.save(providerEvent)
            return
        }

        // Resend does not include headers[] or body in email.received webhooks.
        // message_id is provided as a top-level data field (RFC 2822 Message-ID of the incoming email).
        // In-Reply-To / References are unavailable — threading falls back to subject matching.
        val headers = data.headers ?: emptyList()
        val internetMessageId = data.messageId
            ?: headers.firstOrNull { it.name.equals("Message-ID", ignoreCase = true) }?.value
        val inReplyTo = headers.firstOrNull { it.name.equals("In-Reply-To", ignoreCase = true) }?.value
        val referencesRaw = headers.firstOrNull { it.name.equals("References", ignoreCase = true) }?.value

        log.info(
            "Inbound threading: emailId={} messageId={} inReplyTo={} subject=\"{}\"",
            emailId,
            internetMessageId,
            inReplyTo,
            data.subject,
        )

        val threadResult = threadingService.resolveConversation(
            inReplyTo = inReplyTo,
            referencesRaw = referencesRaw,
            internetMessageId = internetMessageId,
            mailboxId = mailbox.id,
            incomingSubject = data.subject,
        )
        val isNewConversation = threadResult.conversation == null
        log.info(
            "Inbound threading result: emailId={} isNew={} matchedBy={} conversationId={}",
            emailId,
            isNewConversation,
            threadResult.matchedBy,
            threadResult.conversation?.id,
        )

        val conversation = threadResult.conversation ?: conversationRepository.save(
            Conversation(
                mailbox = mailbox,
                subjectNormalized = normalizeSubject(data.subject),
                startedAt = Instant.now(),
                lastMessageAt = Instant.now(),
            ),
        )

        if (!isNewConversation) {
            conversation.lastMessageAt = Instant.now()
            conversationRepository.save(conversation)
        }

        // Resend does not include body in email.received webhooks.
        // Fetch full content via mails.receiving.get() — the inbound-specific API.
        val inboundContent = resendClient.getInboundEmail(emailId)

        val rawHeadersJson = objectMapper.writeValueAsString(headers)

        val message = messageRepository.save(
            Message(
                conversation = conversation,
                direction = MessageDirection.INBOUND,
                status = MessageStatus.RECEIVED_FULL,
                providerMessageId = emailId,
                internetMessageId = internetMessageId?.trim()?.removeSurrounding("<", ">"),
                inReplyTo = inReplyTo?.trim()?.removeSurrounding("<", ">"),
                referencesRaw = referencesRaw,
                subject = data.subject,
                fromEmail = data.from ?: "unknown",
                replyToEmail = data.replyTo?.firstOrNull(),
                textBody = inboundContent?.text ?: data.text,
                htmlBody = inboundContent?.html ?: data.html,
                rawHeadersJson = rawHeadersJson,
                receivedAt = Instant.now(),
            ),
        )

        data.to?.forEach { email ->
            messageRecipientRepository.save(MessageRecipient(message = message, type = RecipientType.TO, email = email))
        }
        data.cc?.forEach { email ->
            messageRecipientRepository.save(MessageRecipient(message = message, type = RecipientType.CC, email = email))
        }
        data.bcc?.forEach { email ->
            messageRecipientRepository.save(
                MessageRecipient(message = message, type = RecipientType.BCC, email = email),
            )
        }

        data.attachments?.forEach { att ->
            messageAttachmentRepository.save(
                MessageAttachment(
                    message = message,
                    providerAttachmentId = att.id,
                    fileName = att.filename ?: "attachment",
                    contentType = att.contentType ?: "application/octet-stream",
                    sizeBytes = att.size,
                ),
            )
        }

        if (isNewConversation) {
            kafkaEventPublisher.saveToOutbox(
                topic = KafkaTopics.EMAIL_CONVERSATION_CREATED,
                aggregateType = "conversation",
                aggregateId = conversation.id.toString(),
                payload = EmailConversationCreatedEvent(
                    conversationId = conversation.id,
                    mailboxId = mailbox.id,
                    firstMessageId = message.id,
                    fromEmail = data.from ?: "unknown",
                    subjectNormalized = normalizeSubject(data.subject),
                    createdAt = Instant.now(),
                ),
            )
        } else {
            kafkaEventPublisher.saveToOutbox(
                topic = KafkaTopics.EMAIL_CONVERSATION_MATCHED,
                aggregateType = "conversation",
                aggregateId = conversation.id.toString(),
                payload = EmailConversationMatchedEvent(
                    messageId = message.id,
                    conversationId = conversation.id,
                    caseId = conversation.caseId,
                    matchedBy = threadResult.matchedBy ?: "unknown",
                    matchedAt = Instant.now(),
                ),
            )
        }

        kafkaEventPublisher.saveToOutbox(
            topic = KafkaTopics.EMAIL_INBOUND_PERSISTED,
            aggregateType = "message",
            aggregateId = message.id.toString(),
            payload = EmailInboundPersistedEvent(
                messageId = message.id,
                conversationId = conversation.id,
                mailboxId = mailbox.id,
                caseId = conversation.caseId,
                fromEmail = data.from ?: "unknown",
                subject = data.subject,
                textBody = message.textBody,
                htmlBody = message.htmlBody,
                internetMessageId = message.internetMessageId,
                isNewConversation = isNewConversation,
                receivedAt = Instant.now(),
            ),
        )

        providerEvent.status = ProviderEventStatus.PROCESSED
        providerEvent.processedAt = Instant.now()
        providerEventRepository.save(providerEvent)

        log.info(
            "Inbound email persisted: messageId={} conversationId={} isNew={}",
            message.id,
            conversation.id,
            isNewConversation,
        )
    }

    @Transactional
    fun processDeliveryEvent(
        payload: ResendWebhookPayload,
        rawBody: String
    ) {
        val data = payload.data
        val emailId = data.emailId ?: run {
            log.warn("Delivery webhook missing emailId: type={}", payload.type)
            return
        }
        val dedupKey = "delivery:${payload.type}:$emailId"

        if (providerEventRepository.existsByDeduplicationKey(dedupKey)) {
            log.info("Duplicate delivery webhook ignored: type={} emailId={}", payload.type, emailId)
            return
        }

        providerEventRepository.save(
            ProviderEvent(
                eventType = payload.type,
                providerEventId = emailId,
                providerMessageId = emailId,
                payloadJson = rawBody,
                deduplicationKey = dedupKey,
                status = ProviderEventStatus.PROCESSING,
            ),
        )

        val message = messageRepository.findByProviderMessageId(emailId)
        if (message == null) {
            log.warn("Delivery event for unknown message: emailId={}", emailId)
            return
        }

        val (newStatus, kafkaTopic, kafkaEvent) = when (payload.type) {
            "email.sent" -> Triple(
                MessageStatus.SENT,
                KafkaTopics.EMAIL_OUTBOUND_SENT,
                EmailOutboundSentEvent(
                    messageId = message.id,
                    conversationId = message.conversation.id,
                    caseId = message.conversation.caseId,
                    providerMessageId = emailId,
                    toEmails = emptyList(),
                    subject = message.subject,
                    sentAt = Instant.now(),
                ),
            )
            "email.delivered" -> Triple(MessageStatus.DELIVERED, null, null)
            "email.bounced" -> Triple(
                MessageStatus.BOUNCED,
                KafkaTopics.EMAIL_OUTBOUND_FAILED,
                EmailOutboundFailedEvent(
                    messageId = message.id,
                    conversationId = message.conversation.id,
                    caseId = message.conversation.caseId,
                    reason = "bounced",
                    failedAt = Instant.now(),
                ),
            )
            "email.complained" -> Triple(MessageStatus.DELIVERED, null, null)
            else -> Triple(null, null, null)
        }

        if (newStatus != null) {
            message.status = newStatus
            if (newStatus == MessageStatus.SENT) message.sentAt = Instant.now()
            messageRepository.save(message)
        }

        if (kafkaTopic != null && kafkaEvent != null) {
            kafkaEventPublisher.saveToOutbox(
                topic = kafkaTopic,
                aggregateType = "message",
                aggregateId = message.id.toString(),
                payload = kafkaEvent,
            )
        }

        log.info("Delivery event processed: type={} emailId={} newStatus={}", payload.type, emailId, newStatus)
    }

    private fun normalizeSubject(subject: String?): String? = threadingService.normalizeSubject(subject)
}
