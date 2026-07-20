package com.waveai.worker.repository;

import com.waveai.worker.entity.TranscriptSegmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface TranscriptSegmentRepository extends JpaRepository<TranscriptSegmentEntity, Long> {

    List<TranscriptSegmentEntity> findBySessionIdOrderBySegmentIdxAsc(String sessionId);

    boolean existsBySessionId(String sessionId);

    @Transactional
    void deleteBySessionId(String sessionId);
}
