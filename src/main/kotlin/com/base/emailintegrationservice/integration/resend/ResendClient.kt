package com.base.emailintegrationservice.integration.resend

import com.base.emailintegrationservice.integration.resend.dto.ResendSendRequest
import com.base.emailintegrationservice.integration.resend.dto.ResendSendResponse
import com.resend.Resend
import com.resend.services.emails.model.CreateEmailOptions
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class ResendClient(
    private val resend: Resend,
) {
    private val log = LoggerFactory.getLogger(ResendClient::class.java)

    /**
     * Sends an email via Resend SDK.
     *
     * @param request the email payload
     * @param idempotencyKey used to prevent duplicate sends; Resend deduplicates within 24h
     * @return the Resend email ID on success
     */
    fun sendEmail(
        request: ResendSendRequest,
        idempotencyKey: String,
    ): ResendSendResponse {
        log.debug(
            "Sending email via Resend: from={} to={} idempotencyKey={}",
            request.from,
            request.to,
            idempotencyKey,
        )

        val params =
            CreateEmailOptions
                .builder()
                .from(request.from)
                .to(request.to)
                .subject(request.subject)
                .apply {
                    request.html?.let { html(it) }
                    request.text?.let { text(it) }
                    request.cc?.let { cc(it) }
                    request.replyTo?.let { replyTo(it) }
                    request.headers?.let { headers(it) }
                }.build()

        val response =
            resend.emails().send(params)
                ?: throw ResendApiException("Empty response from Resend API")

        return ResendSendResponse(id = response.id)
    }
}

class ResendApiException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)
