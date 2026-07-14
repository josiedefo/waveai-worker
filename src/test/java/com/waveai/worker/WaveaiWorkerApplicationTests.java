package com.waveai.worker;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Full-context integration test. A throwaway PostgreSQL is started via
 * Testcontainers and wired in with {@link ServiceConnection}, so the test does
 * not rely on a locally running database or on the {@code DB_URL}/{@code DB_*}
 * environment variables (which override the application.yml defaults when set,
 * even to an empty value).
 *
 * <p>Beyond "the context wires up", this exercises the real Flyway migration and
 * Hibernate {@code ddl-auto: validate} against Postgres under Spring Boot 4 —
 * meaning the entity mappings and the JSONB {@code StringListJsonConverter}
 * migrated to Jackson 3 are validated end to end.
 *
 * <p>Requires Docker to be available on the machine running the build.
 */
@Testcontainers
@SpringBootTest
class WaveaiWorkerApplicationTests {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
        new PostgreSQLContainer<>("postgres:16-alpine");

    @Test
    void contextLoads() {
    }
}
