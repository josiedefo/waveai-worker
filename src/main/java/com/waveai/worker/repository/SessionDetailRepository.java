package com.waveai.worker.repository;

import com.waveai.worker.entity.SessionDetailEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SessionDetailRepository extends JpaRepository<SessionDetailEntity, String> {
}
