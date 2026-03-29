package com.base.emailintegrationservice.domain.enums

enum class MessageStatus {
    // Inbound states
    RECEIVED_META,
    CONTENT_FETCHING,
    RECEIVED_FULL,
    THREAD_MATCHED,
    CASE_LINKED,
    PROCESSING_FAILED,

    // Outbound states
    PENDING_SEND,
    SENDING,
    SENT,
    DELIVERED,
    FAILED,
    BOUNCED,
    CANCELED
}
