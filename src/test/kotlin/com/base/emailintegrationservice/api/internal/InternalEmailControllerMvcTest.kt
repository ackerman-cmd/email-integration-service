package com.base.emailintegrationservice.api.internal

import com.base.emailintegrationservice.service.OutboundEmailService
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(controllers = [InternalEmailController::class])
class InternalEmailControllerMvcTest {
    @Autowired private lateinit var mockMvc: MockMvc

    @MockkBean private lateinit var outboundEmailService: OutboundEmailService

    @Test
    fun `POST send accepts camelCase JSON body`() {
        every { outboundEmailService.sendNew(any(), any(), any(), any(), any(), any(), any()) } returns
            mockk(relaxed = true)

        val body =
            """
            {
              "fromEmail": "support@example.com",
              "to": ["egor@mail.ru"],
              "cc": ["egor@mail.ru"],
              "subject": "string",
              "htmlBody": "string",
              "textBody": "string",
              "createdByUserId": "3fa85f64-5717-4562-b3fc-2c963f66afa6"
            }
            """.trimIndent()

        mockMvc
            .perform(
                post("/internal/emails/send")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body),
            ).andExpect(status().isCreated)
    }
}
