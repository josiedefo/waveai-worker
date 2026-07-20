package com.waveai.worker.mapper;

import com.waveai.worker.dto.FolderDto;
import com.waveai.worker.dto.SessionDetailDto;
import com.waveai.worker.dto.SessionDto;
import com.waveai.worker.dto.TranscriptSegmentDto;
import com.waveai.worker.entity.FolderEntity;
import com.waveai.worker.entity.SessionDetailEntity;
import com.waveai.worker.entity.SessionEntity;
import com.waveai.worker.entity.TranscriptSegmentEntity;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SessionMapper {

    public SessionDto toSessionDto(SessionEntity e) {
        return new SessionDto(
            e.getId(),
            e.getTitle(),
            e.getTimestamp(),
            e.getDurationSeconds(),
            e.getType(),
            e.getPlatform()
        );
    }

    public SessionDetailDto toSessionDetailDto(SessionEntity e) {
        SessionDetailEntity d = e.getDetail();
        return new SessionDetailDto(
            e.getId(),
            e.getTitle(),
            e.getTimestamp(),
            e.getDurationSeconds(),
            e.getType(),
            e.getPlatform(),
            d != null ? d.getLanguage() : null,
            d != null ? d.getSummary() : null,
            d != null ? d.getNotes() : null,
            d != null ? d.getSpeakers() : List.of(),
            d != null ? d.getSessionUrl() : null,
            d != null ? d.getCachedAt() : null
        );
    }

    public FolderDto toFolderDto(FolderEntity e) {
        return new FolderDto(e.getId(), e.getName(), e.getColor(), e.getSessionCount());
    }

    public TranscriptSegmentDto toTranscriptSegmentDto(TranscriptSegmentEntity e) {
        return new TranscriptSegmentDto(
            e.getSpeaker(),
            e.getStartSec(),
            e.getEndSec(),
            e.getText(),
            e.getSegmentIdx()
        );
    }
}
