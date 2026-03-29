package com.base.emailintegrationservice.config

import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.kafka.KafkaProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.serializer.JsonSerializer

@Configuration
class KafkaTemplateConfig(
    private val kafkaProperties: KafkaProperties,
    @Qualifier("kafkaObjectMapper") private val kafkaObjectMapper: ObjectMapper,
) {
    @Bean
    fun kafkaTemplate(): KafkaTemplate<String, Any> {
        val props = kafkaProperties.buildProducerProperties(null)
        val valueSerializer = JsonSerializer<Any>(kafkaObjectMapper)
        valueSerializer.configure(props, false)
        val factory =
            DefaultKafkaProducerFactory<String, Any>(
                props,
                StringSerializer(),
                valueSerializer,
            )
        return KafkaTemplate(factory)
    }
}
