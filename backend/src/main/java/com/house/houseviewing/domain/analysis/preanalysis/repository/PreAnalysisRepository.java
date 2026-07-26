package com.house.houseviewing.domain.analysis.preanalysis.repository;

import com.house.houseviewing.domain.analysis.preanalysis.entity.PreAnalysisEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PreAnalysisRepository extends JpaRepository<PreAnalysisEntity, Long> {

    List<PreAnalysisEntity> findAllByUserId(Long userId);
    boolean existsByUserId(Long userId);
}
