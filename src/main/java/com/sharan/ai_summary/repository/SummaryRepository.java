package com.sharan.ai_summary.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sharan.ai_summary.entity.Summary;

public interface SummaryRepository extends JpaRepository<Summary, Long> {
    Optional<Summary> findByContentHash(String contentHash);
    List<Summary> findByUserIdOrderByCreatedAtDesc(Long userId);
}
