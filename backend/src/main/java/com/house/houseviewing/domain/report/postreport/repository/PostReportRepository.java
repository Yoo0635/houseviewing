package com.house.houseviewing.domain.report.postreport.repository;

import com.house.houseviewing.domain.report.postreport.entity.PostReportEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PostReportRepository extends JpaRepository<PostReportEntity, Long> {
    Optional<PostReportEntity> findByAnalysisId(Long analysisId);
}
