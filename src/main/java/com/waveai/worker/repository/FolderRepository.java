package com.waveai.worker.repository;

import com.waveai.worker.entity.FolderEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FolderRepository extends JpaRepository<FolderEntity, String> {
}
