package com.base.emailintegrationservice.webhook

import com.base.emailintegrationservice.config.ResendProperties
import com.base.emailintegrationservice.webhook.dto.ResendWebhookPayload
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.justRun
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(ResendWebhookController::class)
class ResendWebhookControllerTest {
    @Autowired private lateinit var mockMvc: MockMvc

    @MockkBean private lateinit var resendWebhookService: ResendWebhookService

    @MockkBean private lateinit var resendProperties: ResendProperties

    private val inboundPayload =
        """
        {
          "type": "email.received",
          "created_at": "2024-01-01T00:00:00.000Z",
          "data": {
            "email_id": "em_test123",
            "from": "user@example.com",
            "to": ["support@domain.com"],
            "subject": "Help needed",
            "html": "<p>Hello</p>",
            "text": "Hello"
          }
        }
        """.trimIndent()

    @BeforeEach
    fun setUp() {
        every { resendProperties.webhookSecret } returns ""
        justRun { resendWebhookService.handleAsync(any(), any()) }
    }

    @Test
    fun `returns 200 and delegates to service when signature verification is disabled`() {
        mockMvc
            .perform(
                post("/webhooks/resend")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(inboundPayload),
            ).andExpect(status().isOk)

        verify { resendWebhookService.handleAsync(any(), eq(inboundPayload)) }
    }

    @Test
    fun `returns 400 for invalid JSON body`() {
        mockMvc
            .perform(
                post("/webhooks/resend")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{ not valid json }"),
            ).andExpect(status().isBadRequest)

        verify(exactly = 0) { resendWebhookService.handleAsync(any(), any()) }
    }

    @Test
    fun `returns 400 when signature is required but headers are missing`() {
        every { resendProperties.webhookSecret } returns "whsec_secret123"

        mockMvc
            .perform(
                post("/webhooks/resend")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(inboundPayload),
            ).andExpect(status().isBadRequest)

        verify(exactly = 0) { resendWebhookService.handleAsync(any(), any()) }
    }

    @Test
    fun `passes correctly parsed payload to service`() {
        val payloadSlot = slot<ResendWebhookPayload>()
        justRun { resendWebhookService.handleAsync(capture(payloadSlot), any()) }

        mockMvc
            .perform(
                post("/webhooks/resend")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(inboundPayload),
            ).andExpect(status().isOk)

        assertThat(payloadSlot.captured.type).isEqualTo("email.received")
        assertThat(payloadSlot.captured.data.emailId).isEqualTo("em_test123")
        assertThat(payloadSlot.captured.data.from).isEqualTo("user@example.com")
    }

    @Test
    fun `handles delivery event type`() {
        val deliveryPayload =
            """
            {
              "type": "email.delivered",
              "created_at": "2024-01-01T00:01:00.000Z",
              "data": { "email_id": "em_outbound456" }
            }
            """.trimIndent()

        val payloadSlot = slot<ResendWebhookPayload>()
        justRun { resendWebhookService.handleAsync(capture(payloadSlot), any()) }

        mockMvc
            .perform(
                post("/webhooks/resend")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(deliveryPayload),
            ).andExpect(status().isOk)

        assertThat(payloadSlot.captured.type).isEqualTo("email.delivered")
        assertThat(payloadSlot.captured.data.emailId).isEqualTo("em_outbound456")
    }
}
