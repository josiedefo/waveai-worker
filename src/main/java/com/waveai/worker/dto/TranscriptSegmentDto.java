package com.waveai.worker.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TranscriptSegmentDto(
    String speaker,
    @JsonProperty("startSec") Double startSec,
    @JsonProperty("endSec") Double endSec,
    String text,
    @JsonProperty("segmentIdx") int segmentIdx
) {}
