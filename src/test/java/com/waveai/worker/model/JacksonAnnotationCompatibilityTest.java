package com.waveai.worker.model;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;

import tools.jackson.databind.ObjectMapper;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that the Jackson 2 annotations still used on the API model records
 * ({@code @JsonProperty}, {@code @JsonIgnoreProperties}) are honoured by the
 * Jackson 3 databind that Spring Boot 4 auto-configures.
 *
 * <p>This is the crux of why the {@code model}/{@code dto} records did NOT need
 * changes during the Spring Boot 3.5 -> 4.1 upgrade: Boot 4 ships
 * {@code tools.jackson.core:jackson-databind:3.x} but keeps
 * {@code com.fasterxml.jackson.core:jackson-annotations:2.x}, so the old
 * annotation namespace remains binding-compatible. These tests fail loudly if
 * that ever stops being true.
 *
 * <p>Uses the {@code @JsonTest} slice (no database, no full context) and injects
 * the real Spring-configured Jackson 3 {@link ObjectMapper}.
 */
@JsonTest
class JacksonAnnotationCompatibilityTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void deserializesSnakeCaseJsonProperty_andIgnoresUnknownFields() {
        String json = """
            {
              "id": "sess-1",
              "title": "Weekly Standup",
              "timestamp": "2026-01-15T10:00:00Z",
              "duration_seconds": 1800,
              "type": "recording",
              "platform": "zoom",
              "field_that_does_not_exist": "should be ignored"
            }
            """;

        Session session = objectMapper.readValue(json, Session.class);

        assertThat(session.id()).isEqualTo("sess-1");
        assertThat(session.title()).isEqualTo("Weekly Standup");
        assertThat(session.timestamp()).isEqualTo(Instant.parse("2026-01-15T10:00:00Z"));
        // @JsonProperty("duration_seconds") must map onto durationSeconds
        assertThat(session.durationSeconds()).isEqualTo(1800L);
        assertThat(session.type()).isEqualTo("recording");
        assertThat(session.platform()).isEqualTo("zoom");
    }

    @Test
    void serializesUsingJsonPropertyName() {
        Session session = new Session(
            "sess-2", "Retro", Instant.parse("2026-02-01T09:30:00Z"),
            3600L, "recording", "meet");

        String json = objectMapper.writeValueAsString(session);

        // Field must be written back out as snake_case per @JsonProperty
        assertThat(json).contains("\"duration_seconds\":3600");
        assertThat(json).doesNotContain("durationSeconds");
    }

    @Test
    void sessionDetail_mapsSessionUrlAndSpeakersArray() {
        String json = """
            {
              "id": "sess-3",
              "title": "Design Review",
              "duration_seconds": 900,
              "session_url": "https://wave.co/s/sess-3",
              "speakers": ["Alice", "Bob", "Carol"],
              "unexpected": true
            }
            """;

        SessionDetail detail = objectMapper.readValue(json, SessionDetail.class);

        assertThat(detail.id()).isEqualTo("sess-3");
        assertThat(detail.durationSeconds()).isEqualTo(900L);
        assertThat(detail.sessionUrl()).isEqualTo("https://wave.co/s/sess-3");
        assertThat(detail.speakers()).containsExactly("Alice", "Bob", "Carol");
    }

    @Test
    void sessionsResponse_deserializesNestedList() {
        String json = """
            {
              "sessions": [
                {"id": "a", "duration_seconds": 60},
                {"id": "b", "duration_seconds": 120}
              ]
            }
            """;

        SessionsResponse response = objectMapper.readValue(json, SessionsResponse.class);

        assertThat(response.sessions()).hasSize(2);
        assertThat(response.sessions().get(0).id()).isEqualTo("a");
        assertThat(response.sessions().get(1).durationSeconds()).isEqualTo(120L);
    }
}
