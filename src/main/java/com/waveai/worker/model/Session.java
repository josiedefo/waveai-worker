package com.waveai.worker.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Session(
    String id,
    String title,
    Instant timestamp,
    @JsonProperty("duration_seconds") long durationSeconds,
    String type,
    String platform
) {}
