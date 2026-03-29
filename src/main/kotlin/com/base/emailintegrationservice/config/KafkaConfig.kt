package com.base.emailintegrationservice.config

import org.apache.kafka.clients.admin.NewTopic
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.config.TopicBuilder

@Configuration
class KafkaConfig {
    @Bean
    fun emailInboundReceivedTopic(): NewTopic =
        TopicBuilder
            .name(KafkaTopics.EMAIL_INBOUND_RECEIVED)
            .partitions(3)
            .replicas(1)
            .build()

    @Bean
    fun emailInboundPersistedTopic(): NewTopic =
        TopicBuilder
            .name(KafkaTopics.EMAIL_INBOUND_PERSISTED)
            .partitions(3)
            .replicas(1)
            .build()

    @Bean
    fun emailInboundFailedTopic(): NewTopic =
        TopicBuilder
            .name(KafkaTopics.EMAIL_INBOUND_FAILED)
            .partitions(1)
            .replicas(1)
            .build()

    @Bean
    fun emailConversationCreatedTopic(): NewTopic =
        TopicBuilder
            .name(KafkaTopics.EMAIL_CONVERSATION_CREATED)
            .partitions(3)
            .replicas(1)
            .build()

    @Bean
    fun emailConversationMatchedTopic(): NewTopic =
        TopicBuilder
            .name(KafkaTopics.EMAIL_CONVERSATION_MATCHED)
            .partitions(3)
            .replicas(1)
            .build()

    @Bean
    fun emailOutboundRequestedTopic(): NewTopic =
        TopicBuilder
            .name(KafkaTopics.EMAIL_OUTBOUND_REQUESTED)
            .partitions(3)
            .replicas(1)
            .build()

    @Bean
    fun emailOutboundSentTopic(): NewTopic =
        TopicBuilder
            .name(KafkaTopics.EMAIL_OUTBOUND_SENT)
            .partitions(3)
            .replicas(1)
            .build()

    @Bean
    fun emailOutboundFailedTopic(): NewTopic =
        TopicBuilder
            .name(KafkaTopics.EMAIL_OUTBOUND_FAILED)
            .partitions(1)
            .replicas(1)
            .build()
}
