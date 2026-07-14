package com.waveai.worker.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record FolderDto(
    String id,
    String name,
    String color,
    @JsonProperty("sessionCount") int sessionCount
) {}
