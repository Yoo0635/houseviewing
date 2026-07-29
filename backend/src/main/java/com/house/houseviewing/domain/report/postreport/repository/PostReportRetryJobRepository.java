package com.house.houseviewing.domain.report.postreport.repository;

import com.house.houseviewing.domain.report.postreport.entity.PostReportRetryJobEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PostReportRetryJobRepository extends JpaRepository<PostReportRetryJobEntity, Long> {
    Optional<PostReportRetryJobEntity> findByAnalysisId(Long analysisId);
}
