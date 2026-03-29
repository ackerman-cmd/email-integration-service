package com.base.emailintegrationservice.config

object KafkaTopics {
    const val EMAIL_INBOUND_RECEIVED = "email.inbound.received"
    const val EMAIL_INBOUND_PERSISTED = "email.inbound.persisted"
    const val EMAIL_INBOUND_FAILED = "email.inbound.failed"
    const val EMAIL_CONVERSATION_CREATED = "email.conversation.created"
    const val EMAIL_CONVERSATION_MATCHED = "email.conversation.matched"
    const val EMAIL_OUTBOUND_REQUESTED = "email.outbound.requested"
    const val EMAIL_OUTBOUND_SENT = "email.outbound.sent"
    const val EMAIL_OUTBOUND_FAILED = "email.outbound.failed"
}
