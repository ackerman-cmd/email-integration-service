package com.base.emailintegrationservice.repository

import com.base.emailintegrationservice.domain.entity.OutboxEvent
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.time.Instant
import java.util.UUID

interface OutboxEventRepository : JpaRepository<OutboxEvent, UUID> {
    /**
     * Returns pending events ready to publish (status = PENDING, retry window elapsed).
     * Uses Pageable for LIMIT to stay compliant with Hibernate 6 HQL restrictions.
     */
    @Query(
        """
        SELECT e FROM OutboxEvent e
        WHERE e.status = 'PENDING'
          AND (e.nextRetryAt IS NULL OR e.nextRetryAt <= :now)
        ORDER BY e.createdAt ASC
        """
    )
    fun findReadyToPublish(
        now: Instant,
        pageable: Pageable
    ): List<OutboxEvent>

    fun findByAggregateTypeAndAggregateId(
        aggregateType: String,
        aggregateId: String
    ): List<OutboxEvent>
}
