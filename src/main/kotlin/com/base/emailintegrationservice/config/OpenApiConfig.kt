package com.base.emailintegrationservice.config

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.servers.Server
import io.swagger.v3.oas.models.tags.Tag
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfig {
    @Bean
    fun openApi(): OpenAPI =
        OpenAPI()
            .info(
                Info()
                    .title("Email Integration Service")
                    .description(
                        """
                        Интеграционный сервис почтового канала.
                        
                        Отвечает за:
                        - Приём входящих писем через Resend webhook
                        - Отправку исходящих писем (send / reply / forward) через Resend API
                        - Email-threading по RFC 2822 заголовкам (In-Reply-To, References)
                        - Хранение переписки и метаданных вложений
                        - Публикацию интеграционных событий в Kafka через Outbox pattern
                        
                        Потребители API: arm-support-service (internal endpoints).
                        """.trimIndent()
                    ).version("1.0.0")
                    .contact(Contact().name("Platform Team"))
            ).servers(
                listOf(
                    Server().url("/").description("Текущий хост (тот же, что и Swagger UI)"),
                )
            ).tags(
                listOf(
                    Tag().name("Webhook").description("Intake endpoint для событий от Resend"),
                    Tag().name("Internal — Emails").description("Команды отправки от arm-support-service"),
                    Tag().name("Internal — Conversations").description("Чтение переписки arm-support-service"),
                )
            )
}
