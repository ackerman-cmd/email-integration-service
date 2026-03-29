package com.base.emailintegrationservice.api.internal.dto

import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.json.JsonTest
import java.util.UUID

@JsonTest
class SendEmailRequestJsonTest {
    @Autowired private lateinit var objectMapper: ObjectMapper

    @Test
    fun `deserializes camelCase JSON like Swagger`() {
        val json =
            """
            {
              "mailboxId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
              "to": ["egor@mail.ru"],
              "cc": ["egor@mail.ru"],
              "subject": "string",
              "htmlBody": "string",
              "textBody": "string",
              "createdByUserId": "3fa85f64-5717-4562-b3fc-2c963f66afa6"
            }
            """.trimIndent()

        val req = objectMapper.readValue(json, SendEmailRequest::class.java)

        assertThat(req.mailboxId).isEqualTo(UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa6"))
        assertThat(req.to).containsExactly("egor@mail.ru")
        assertThat(req.cc).containsExactly("egor@mail.ru")
        assertThat(req.subject).isEqualTo("string")
    }
}
