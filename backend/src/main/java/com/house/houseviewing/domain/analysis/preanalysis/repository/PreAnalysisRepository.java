package com.house.houseviewing.domain.analysis.preanalysis.repository;

import com.house.houseviewing.domain.analysis.preanalysis.entity.PreAnalysisEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PreAnalysisRepository extends JpaRepository<PreAnalysisEntity, Long> {

    List<PreAnalysisEntity> findAllByUserId(Long userId);
    boolean existsByUserId(Long userId);
    Optional<PreAnalysisEntity> findByUserIdAndFreeDiagnosisRequestId(Long userId, String freeDiagnosisRequestId);
}
