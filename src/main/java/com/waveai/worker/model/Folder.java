package com.waveai.worker.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Folder(
    String id,
    String name,
    String color,
    @JsonProperty("session_count") int sessionCount
) {}
