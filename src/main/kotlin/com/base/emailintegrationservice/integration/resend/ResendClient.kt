package com.base.emailintegrationservice.integration.resend

import com.base.emailintegrationservice.integration.resend.dto.ResendSendRequest
import com.base.emailintegrationservice.integration.resend.dto.ResendSendResponse
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.body

@Component
class ResendClient(
    @Qualifier("resendRestClient") private val restClient: RestClient
) {
    private val log = LoggerFactory.getLogger(ResendClient::class.java)

    /**
     * Sends an email via Resend API.
     *
     * @param request the email payload
     * @param idempotencyKey used to prevent duplicate sends; Resend deduplicates within 24h
     * @return the Resend email ID on success
     */
    fun sendEmail(
        request: ResendSendRequest,
        idempotencyKey: String
    ): ResendSendResponse {
        log.debug("Sending email via Resend: from={} to={} idempotencyKey={}", request.from, request.to, idempotencyKey)

        return restClient
            .post()
            .uri("/emails")
            .header("Idempotency-Key", idempotencyKey)
            .body(request)
            .retrieve()
            .body<ResendSendResponse>()
            ?: throw ResendApiException("Empty response from Resend API")
    }
}

class ResendApiException(
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause)
