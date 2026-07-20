package com.waveai.worker.repository;

import com.waveai.worker.entity.FolderEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.Optional;

public interface FolderRepository extends JpaRepository<FolderEntity, String> {

    @Query("select max(f.cachedAt) from FolderEntity f")
    Optional<Instant> findMaxCachedAt();
}
