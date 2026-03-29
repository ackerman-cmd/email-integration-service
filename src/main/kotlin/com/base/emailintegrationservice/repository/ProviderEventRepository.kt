package com.base.emailintegrationservice.repository

import com.base.emailintegrationservice.domain.entity.ProviderEvent
import com.base.emailintegrationservice.domain.enums.ProviderEventStatus
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface ProviderEventRepository : JpaRepository<ProviderEvent, UUID> {
    fun existsByDeduplicationKey(deduplicationKey: String): Boolean

    fun findByDeduplicationKey(deduplicationKey: String): ProviderEvent?

    fun findByProviderMessageId(providerMessageId: String): List<ProviderEvent>

    fun findByStatus(status: ProviderEventStatus): List<ProviderEvent>
}
