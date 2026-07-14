package com.waveai.worker.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

public record SessionDto(
    String id,
    String title,
    Instant timestamp,
    @JsonProperty("durationSeconds") long durationSeconds,
    String type,
    String platform
) {}
