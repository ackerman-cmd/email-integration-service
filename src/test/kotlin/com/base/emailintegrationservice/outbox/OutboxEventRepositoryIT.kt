package com.base.emailintegrationservice.outbox

import com.base.emailintegrationservice.domain.entity.OutboxEvent
import com.base.emailintegrationservice.domain.enums.OutboxEventStatus
import com.base.emailintegrationservice.repository.OutboxEventRepository
import com.base.emailintegrationservice.support.AbstractRepositoryTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import java.time.Instant
import java.util.UUID

class OutboxEventRepositoryIT : AbstractRepositoryTest() {
    @Autowired private lateinit var outboxEventRepository: OutboxEventRepository

    @Test
    fun `findReadyToPublish returns only PENDING events with elapsed retry window`() {
        val pendingReady = outboxEvent(status = OutboxEventStatus.PENDING, nextRetryAt = null)
        val pendingNotReady = outboxEvent(
            status = OutboxEventStatus.PENDING,
            nextRetryAt = Instant.now().plusSeconds(300),
        )
        val alreadySent = outboxEvent(status = OutboxEventStatus.SENT)
        outboxEventRepository.saveAll(listOf(pendingReady, pendingNotReady, alreadySent))

        val result = outboxEventRepository.findReadyToPublish(Instant.now(), PageRequest.of(0, 50))

        assertThat(result).hasSize(1)
        assertThat(result.first().id).isEqualTo(pendingReady.id)
    }

    @Test
    fun `findReadyToPublish respects page size`() {
        val events = (1..10).map { outboxEvent(status = OutboxEventStatus.PENDING) }
        outboxEventRepository.saveAll(events)

        val result = outboxEventRepository.findReadyToPublish(Instant.now(), PageRequest.of(0, 3))

        assertThat(result).hasSize(3)
    }

    @Test
    fun `findReadyToPublish includes events whose next_retry_at has elapsed`() {
        val pastRetry = outboxEvent(
            status = OutboxEventStatus.PENDING,
            nextRetryAt = Instant.now().minusSeconds(60),
        )
        outboxEventRepository.save(pastRetry)

        val result = outboxEventRepository.findReadyToPublish(Instant.now(), PageRequest.of(0, 10))

        assertThat(result).hasSize(1)
    }

    @Test
    fun `SENT and FAILED events are never returned`() {
        outboxEventRepository.saveAll(
            listOf(
                outboxEvent(status = OutboxEventStatus.SENT),
                outboxEvent(status = OutboxEventStatus.FAILED),
            ),
        )

        val result = outboxEventRepository.findReadyToPublish(Instant.now(), PageRequest.of(0, 50))

        assertThat(result).isEmpty()
    }

    private fun outboxEvent(
        status: OutboxEventStatus = OutboxEventStatus.PENDING,
        nextRetryAt: Instant? = null,
    ) = OutboxEvent(
        aggregateType = "message",
        aggregateId = UUID.randomUUID().toString(),
        eventType = "email.outbound.sent",
        payloadJson = """{"test": true}""",
        status = status,
        nextRetryAt = nextRetryAt,
    )
}
