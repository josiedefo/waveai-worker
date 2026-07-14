package com.waveai.worker.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.List;

public record SessionDetailDto(
    String id,
    String title,
    Instant timestamp,
    @JsonProperty("durationSeconds") long durationSeconds,
    String type,
    String platform,
    String language,
    String summary,
    String notes,
    List<String> speakers,
    @JsonProperty("sessionUrl") String sessionUrl
) {}
