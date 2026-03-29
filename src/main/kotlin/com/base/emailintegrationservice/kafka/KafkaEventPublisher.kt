package com.base.emailintegrationservice.kafka

import com.base.emailintegrationservice.domain.entity.OutboxEvent
import com.base.emailintegrationservice.repository.OutboxEventRepository
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Component
class KafkaEventPublisher(
    private val outboxEventRepository: OutboxEventRepository,
    @Qualifier("kafkaObjectMapper") private val objectMapper: ObjectMapper,
) {
    @Transactional(propagation = Propagation.REQUIRED)
    fun saveToOutbox(
        topic: String,
        aggregateType: String,
        aggregateId: String,
        payload: Any,
    ): OutboxEvent {
        val event = OutboxEvent(
            aggregateType = aggregateType,
            aggregateId = aggregateId,
            eventType = topic,
            payloadJson = objectMapper.writeValueAsString(payload),
        )
        return outboxEventRepository.save(event)
    }
}
