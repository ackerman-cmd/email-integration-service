package com.base.emailintegrationservice.integration.resend

import com.base.emailintegrationservice.integration.resend.dto.ResendEmailContent
import com.base.emailintegrationservice.integration.resend.dto.ResendSendRequest
import com.base.emailintegrationservice.integration.resend.dto.ResendSendResponse
import com.resend.Resend
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.body

@Component
class ResendClient(
    private val resend: Resend,
    @Qualifier("resendRestClient") private val restClient: RestClient,
) {
    private val log = LoggerFactory.getLogger(ResendClient::class.java)

    /**
     * Retrieves full inbound email content by Resend email ID.
     * Uses resend.mails().receiving().get() — the inbound-specific API,
     * NOT resend.emails().get() which only works for outbound sent emails.
     */
    fun getInboundEmail(emailId: String): ResendEmailContent? =
        try {
            val email = resend.receiving().get(emailId)
            ResendEmailContent(
                id = email.id,
                html = email.html,
                text = email.text,
            )
        } catch (ex: Exception) {
            log.warn("Failed to retrieve inbound email content emailId={}: {}", emailId, ex.message)
            null
        }

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

        return restClient
            .post()
            .uri("/emails")
            .header("Idempotency-Key", idempotencyKey)
            .contentType(MediaType.APPLICATION_JSON)
            .body(request)
            .retrieve()
            .body<ResendSendResponse>()
            ?: throw ResendApiException("Empty response from Resend API")
    }
}

class ResendApiException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)
