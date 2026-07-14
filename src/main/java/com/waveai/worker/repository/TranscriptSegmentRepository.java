package com.waveai.worker.repository;

import com.waveai.worker.entity.TranscriptSegmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TranscriptSegmentRepository extends JpaRepository<TranscriptSegmentEntity, Long> {

    List<TranscriptSegmentEntity> findBySessionIdOrderBySegmentIdxAsc(String sessionId);

    void deleteBySessionId(String sessionId);
}
