package com.base.emailintegrationservice.webhook

import com.base.emailintegrationservice.config.ResendProperties
import com.base.emailintegrationservice.webhook.dto.ResendWebhookPayload
import com.fasterxml.jackson.databind.ObjectMapper
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Thin webhook intake endpoint. Validates signature, saves raw event, returns 200 immediately.
 * All heavy processing is delegated asynchronously to ResendWebhookService.
 */
@Tag(name = "Webhook")
@RestController
@RequestMapping("/webhooks/resend")
class ResendWebhookController(
    private val resendWebhookService: ResendWebhookService,
    private val resendProperties: ResendProperties,
    private val objectMapper: ObjectMapper
) {
    private val log = LoggerFactory.getLogger(ResendWebhookController::class.java)

    @Operation(
        summary = "Resend webhook intake",
        description = "Принимает события email.received / email.sent / email.bounced от Resend. " +
            "Возвращает 200 немедленно — вся обработка асинхронна."
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Событие принято"),
        ApiResponse(responseCode = "400", description = "Невалидный JSON или подпись")
    )
    @PostMapping
    fun handleWebhook(
        @RequestBody rawBody: String,
        @RequestHeader(value = "svix-id", required = false) svixId: String?,
        @RequestHeader(value = "svix-timestamp", required = false) svixTimestamp: String?,
        @RequestHeader(value = "svix-signature", required = false) svixSignature: String?
    ): ResponseEntity<Void> {
        if (resendProperties.webhookSecret.isNotBlank()) {
            try {
                verifySignature(rawBody, svixId, svixTimestamp, svixSignature)
            } catch (ex: IllegalArgumentException) {
                log.warn("Webhook signature validation failed: {}", ex.message)
                return ResponseEntity.badRequest().build()
            }
        }

        val payload = try {
            objectMapper.readValue(rawBody, ResendWebhookPayload::class.java)
        } catch (ex: Exception) {
            log.warn("Failed to parse Resend webhook body: {}", ex.message)
            return ResponseEntity.badRequest().build()
        }

        log.info("Received Resend webhook type={} emailId={}", payload.type, payload.data.emailId)

        resendWebhookService.handleAsync(payload, rawBody)

        return ResponseEntity.ok().build()
    }

    private fun verifySignature(
        body: String,
        svixId: String?,
        svixTimestamp: String?,
        svixSignature: String?
    ) {
        require(!svixId.isNullOrBlank() && !svixTimestamp.isNullOrBlank() && !svixSignature.isNullOrBlank()) {
            "Missing Svix signature headers"
        }

        val signingInput = "$svixId.$svixTimestamp.$body"
        val secret = resendProperties.webhookSecret.removePrefix("whsec_")
        val decodedSecret = java.util.Base64
            .getDecoder()
            .decode(secret)

        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(decodedSecret, "HmacSHA256"))
        val computed = java.util.Base64.getEncoder().encodeToString(
            mac.doFinal(signingInput.toByteArray(Charsets.UTF_8))
        )

        val isValid = svixSignature.split(" ").any { sig ->
            sig.removePrefix("v1,") == computed
        }

        require(isValid) { "Invalid webhook signature" }
    }
}
