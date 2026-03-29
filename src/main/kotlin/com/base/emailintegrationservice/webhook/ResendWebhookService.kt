package com.base.emailintegrationservice.webhook

import com.base.emailintegrationservice.config.KafkaTopics
import com.base.emailintegrationservice.domain.entity.*
import com.base.emailintegrationservice.domain.enums.*
import com.base.emailintegrationservice.kafka.KafkaEventPublisher
import com.base.emailintegrationservice.kafka.event.*
import com.base.emailintegrationservice.repository.*
import com.base.emailintegrationservice.service.ThreadingService
import com.base.emailintegrationservice.webhook.dto.ResendWebhookPayload
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class ResendWebhookService(
    private val providerEventRepository: ProviderEventRepository,
    private val mailboxRepository: MailboxRepository,
    private val conversationRepository: ConversationRepository,
    private val messageRepository: MessageRepository,
    private val messageRecipientRepository: MessageRecipientRepository,
    private val messageAttachmentRepository: MessageAttachmentRepository,
    private val threadingService: ThreadingService,
    private val kafkaEventPublisher: KafkaEventPublisher,
    private val objectMapper: ObjectMapper,
) {
    private val log = LoggerFactory.getLogger(ResendWebhookService::class.java)

    @Async
    fun handleAsync(
        payload: ResendWebhookPayload,
        rawBody: String
    ) {
        try {
            when {
                payload.type == "email.received" -> processInboundEmail(payload, rawBody)
                payload.type in DELIVERY_EVENT_TYPES -> processDeliveryEvent(payload, rawBody)
                else -> log.warn("Unhandled Resend webhook type: {}", payload.type)
            }
        } catch (ex: Exception) {
            log.error(
                "Error processing Resend webhook type={} emailId={}: {}",
                payload.type,
                payload.data.emailId,
                ex.message,
                ex,
            )
        }
    }

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

        val headers = data.headers ?: emptyList()
        val internetMessageId = headers.firstOrNull { it.name.equals("Message-ID", ignoreCase = true) }?.value
        val inReplyTo = headers.firstOrNull { it.name.equals("In-Reply-To", ignoreCase = true) }?.value
        val referencesRaw = headers.firstOrNull { it.name.equals("References", ignoreCase = true) }?.value

        val threadResult = threadingService.resolveConversation(inReplyTo, referencesRaw, internetMessageId)
        val isNewConversation = threadResult.conversation == null

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
                textBody = data.text,
                htmlBody = data.html,
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
                MessageRecipient(message = message, type = RecipientType.BCC, email = email)
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

    private fun normalizeSubject(subject: String?): String? =
        subject
            ?.removePrefix("Re: ")
            ?.removePrefix("RE: ")
            ?.removePrefix("Fwd: ")
            ?.removePrefix("FWD: ")
            ?.trim()

    companion object {
        private val DELIVERY_EVENT_TYPES = setOf("email.sent", "email.delivered", "email.bounced", "email.complained")
    }
}
