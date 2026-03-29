package com.base.emailintegrationservice.support

import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

/**
 * Base class for JPA slice tests (@DataJpaTest).
 * Uses a real PostgreSQL container so Liquibase migrations and jsonb types work correctly.
 * Kafka is intentionally excluded — not needed for repository tests.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
abstract class AbstractRepositoryTest {
    companion object {
        @Container
        @ServiceConnection
        @JvmStatic
        val postgres: PostgreSQLContainer<*> =
            PostgreSQLContainer("postgres:17-alpine")
                .withDatabaseName("email_service")
                .withUsername("admin")
                .withPassword("secret")
                .withInitScript("db/init.sql")
    }
}
