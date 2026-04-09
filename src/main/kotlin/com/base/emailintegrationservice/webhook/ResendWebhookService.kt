package com.base.emailintegrationservice.webhook

import com.base.emailintegrationservice.webhook.dto.ResendWebhookPayload
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service

@Service
class ResendWebhookService(
    private val resendWebhookTransactionalProcessor: ResendWebhookTransactionalProcessor,
) {
    private val log = LoggerFactory.getLogger(ResendWebhookService::class.java)

    @Async
    fun handleAsync(
        payload: ResendWebhookPayload,
        rawBody: String
    ) {
        try {
            when {
                payload.type == "email.received" ->
                    resendWebhookTransactionalProcessor.processInboundEmail(payload, rawBody)
                payload.type in DELIVERY_EVENT_TYPES ->
                    resendWebhookTransactionalProcessor.processDeliveryEvent(payload, rawBody)
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

    companion object {
        private val DELIVERY_EVENT_TYPES = setOf("email.sent", "email.delivered", "email.bounced", "email.complained")
    }
}
