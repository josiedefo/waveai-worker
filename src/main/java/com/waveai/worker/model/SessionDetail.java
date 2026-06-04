package com.waveai.worker.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionDetail(
    String id,
    String title,
    Instant timestamp,
    @JsonProperty("duration_seconds") long durationSeconds,
    String type,
    String platform,
    String language,
    String summary,
    String notes,
    List<String> speakers,
    @JsonProperty("session_url") String sessionUrl
) {}
