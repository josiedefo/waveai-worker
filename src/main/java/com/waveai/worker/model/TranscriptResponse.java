package com.waveai.worker.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TranscriptResponse(List<TranscriptSegment> segments) {}
