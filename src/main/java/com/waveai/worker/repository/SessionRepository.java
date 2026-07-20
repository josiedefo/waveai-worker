package com.waveai.worker.repository;

import com.waveai.worker.entity.SessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.Optional;

public interface SessionRepository extends JpaRepository<SessionEntity, String> {

    @Query("select max(s.cachedAt) from SessionEntity s")
    Optional<Instant> findMaxCachedAt();
}
