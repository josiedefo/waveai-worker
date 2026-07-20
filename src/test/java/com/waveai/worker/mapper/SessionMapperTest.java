package com.waveai.worker.mapper;

import com.waveai.worker.dto.FolderDto;
import com.waveai.worker.dto.SessionDetailDto;
import com.waveai.worker.dto.SessionDto;
import com.waveai.worker.entity.FolderEntity;
import com.waveai.worker.entity.SessionDetailEntity;
import com.waveai.worker.entity.SessionEntity;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pure unit tests for {@link SessionMapper} (no Spring context, no database).
 */
class SessionMapperTest {

    private final SessionMapper mapper = new SessionMapper();

    @Test
    void toSessionDto_copiesAllFields() {
        SessionEntity entity = new SessionEntity();
        entity.setId("s1");
        entity.setTitle("Standup");
        entity.setTimestamp(Instant.parse("2026-01-15T10:00:00Z"));
        entity.setDurationSeconds(1800L);
        entity.setType("recording");
        entity.setPlatform("zoom");

        SessionDto dto = mapper.toSessionDto(entity);

        assertThat(dto.id()).isEqualTo("s1");
        assertThat(dto.title()).isEqualTo("Standup");
        assertThat(dto.timestamp()).isEqualTo(Instant.parse("2026-01-15T10:00:00Z"));
        assertThat(dto.durationSeconds()).isEqualTo(1800L);
        assertThat(dto.type()).isEqualTo("recording");
        assertThat(dto.platform()).isEqualTo("zoom");
    }

    @Test
    void toSessionDetailDto_withDetail_mapsNestedFields() {
        SessionEntity entity = new SessionEntity();
        entity.setId("s2");
        entity.setTitle("Design Review");
        entity.setDurationSeconds(900L);
        entity.setType("recording");
        entity.setPlatform("meet");

        SessionDetailEntity detail = new SessionDetailEntity();
        detail.setLanguage("en");
        detail.setSummary("A summary");
        detail.setNotes("Some notes");
        detail.setSpeakers(List.of("Alice", "Bob"));
        detail.setSessionUrl("https://wave.co/s/s2");
        detail.setCachedAt(Instant.parse("2026-01-15T11:00:00Z"));
        entity.setDetail(detail);

        SessionDetailDto dto = mapper.toSessionDetailDto(entity);

        assertThat(dto.id()).isEqualTo("s2");
        assertThat(dto.language()).isEqualTo("en");
        assertThat(dto.summary()).isEqualTo("A summary");
        assertThat(dto.notes()).isEqualTo("Some notes");
        assertThat(dto.speakers()).containsExactly("Alice", "Bob");
        assertThat(dto.sessionUrl()).isEqualTo("https://wave.co/s/s2");
        assertThat(dto.cachedAt()).isEqualTo(Instant.parse("2026-01-15T11:00:00Z"));
    }

    @Test
    void toSessionDetailDto_withoutDetail_usesNullsAndEmptySpeakers() {
        SessionEntity entity = new SessionEntity();
        entity.setId("s3");
        entity.setDurationSeconds(0L);
        // no detail set

        SessionDetailDto dto = mapper.toSessionDetailDto(entity);

        assertThat(dto.id()).isEqualTo("s3");
        assertThat(dto.language()).isNull();
        assertThat(dto.summary()).isNull();
        assertThat(dto.notes()).isNull();
        assertThat(dto.sessionUrl()).isNull();
        assertThat(dto.speakers()).isEmpty();
    }

    @Test
    void toFolderDto_copiesAllFields() {
        FolderEntity entity = new FolderEntity();
        entity.setId("f1");
        entity.setName("Work");
        entity.setColor("#ff0000");
        entity.setSessionCount(7);

        FolderDto dto = mapper.toFolderDto(entity);

        assertThat(dto.id()).isEqualTo("f1");
        assertThat(dto.name()).isEqualTo("Work");
        assertThat(dto.color()).isEqualTo("#ff0000");
        assertThat(dto.sessionCount()).isEqualTo(7);
    }
}
