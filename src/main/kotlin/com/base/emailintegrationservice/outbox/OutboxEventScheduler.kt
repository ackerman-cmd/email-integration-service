package com.base.emailintegrationservice.outbox

import com.base.emailintegrationservice.domain.enums.OutboxEventStatus
import com.base.emailintegrationservice.repository.OutboxEventRepository
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.domain.PageRequest
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.concurrent.TimeUnit

@Component
class OutboxEventScheduler(
    private val outboxEventRepository: OutboxEventRepository,
    private val kafkaTemplate: KafkaTemplate<String, Any>,
    @Qualifier("kafkaObjectMapper") private val objectMapper: ObjectMapper,
) {
    private val log = LoggerFactory.getLogger(OutboxEventScheduler::class.java)
    private val batchSize = 50
    private val maxRetries = 5

    @Scheduled(fixedDelay = 5_000)
    @Transactional
    fun processOutboxEvents() {
        val events = outboxEventRepository.findReadyToPublish(Instant.now(), PageRequest.of(0, batchSize))
        if (events.isEmpty()) return

        log.debug("Processing {} outbox events", events.size)

        for (event in events) {
            try {
                val payload = objectMapper.readValue(event.payloadJson, Map::class.java)
                kafkaTemplate
                    .send(event.eventType, event.aggregateId, payload)
                    .get(10, TimeUnit.SECONDS)
                event.status = OutboxEventStatus.SENT
                log.debug("Published outbox event id={} topic={}", event.id, event.eventType)
            } catch (ex: Exception) {
                log.error("Failed to publish outbox event id={} topic={}: {}", event.id, event.eventType, ex.message)
                event.retryCount++
                if (event.retryCount >= maxRetries) {
                    event.status = OutboxEventStatus.FAILED
                } else {
                    val backoffSeconds = (1L shl event.retryCount) * 10L
                    event.nextRetryAt = Instant.now().plusSeconds(backoffSeconds)
                }
            }
        }
    }
}
