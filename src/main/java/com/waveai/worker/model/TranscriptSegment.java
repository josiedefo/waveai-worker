package com.waveai.worker.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TranscriptSegment(
    String speaker,
    Double start,
    Double end,
    String text
) {}
