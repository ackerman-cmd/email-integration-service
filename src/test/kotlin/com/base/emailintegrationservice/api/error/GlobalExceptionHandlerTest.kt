package com.base.emailintegrationservice.api.error

import com.base.emailintegrationservice.api.internal.InternalConversationController
import com.base.emailintegrationservice.repository.ConversationRepository
import com.base.emailintegrationservice.repository.MessageAttachmentRepository
import com.base.emailintegrationservice.repository.MessageRecipientRepository
import com.base.emailintegrationservice.repository.MessageRepository
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.Optional
import java.util.UUID

@WebMvcTest(InternalConversationController::class)
class GlobalExceptionHandlerNotFoundTest {
    @Autowired private lateinit var mockMvc: MockMvc

    @MockkBean private lateinit var conversationRepository: ConversationRepository

    @MockkBean private lateinit var messageRepository: MessageRepository

    @MockkBean private lateinit var messageRecipientRepository: MessageRecipientRepository

    @MockkBean private lateinit var messageAttachmentRepository: MessageAttachmentRepository

    @Test
    fun `response status exception is serialized for the client`() {
        val id = UUID.randomUUID()
        every { conversationRepository.findById(id) } returns Optional.empty()

        mockMvc
            .perform(get("/internal/conversations/$id"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.code").value("NOT_FOUND"))
            .andExpect(jsonPath("$.message").value("Conversation not found: $id"))
    }
}

@WebMvcTest(ValidationProbeController::class)
class GlobalExceptionHandlerValidationTest {
    @Autowired private lateinit var mockMvc: MockMvc

    @Test
    fun `validation errors include field details`() {
        val body = """{"title":""}"""

        mockMvc
            .perform(
                post("/__probe")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body),
            ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
            .andExpect(jsonPath("$.errors[0].field").value("title"))
    }
}
