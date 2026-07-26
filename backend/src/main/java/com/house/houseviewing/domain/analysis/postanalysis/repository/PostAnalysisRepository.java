package com.house.houseviewing.domain.analysis.postanalysis.repository;

import com.house.houseviewing.domain.analysis.postanalysis.entity.PostAnalysisEntity;
import com.house.houseviewing.domain.analysis.postanalysis.enums.AnalysisType;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface PostAnalysisRepository extends JpaRepository<PostAnalysisEntity, Long> {

    Optional<PostAnalysisEntity> findTopByHouseIdOrderByCreatedAtDesc(Long houseId);

    @Query("""
        SELECT ra
        FROM PostAnalysisEntity ra
        JOIN ra.contract c
        JOIN c.house h
        JOIN h.user u
        WHERE u.id = :userId
        ORDER BY ra.createdAt DESC
    """)
    List<PostAnalysisEntity> findAllByUserId(Long userId);

    List<PostAnalysisEntity> findTop2ByContractHouseIdOrderByCreatedAtDesc(Long houseId);

    @Query("""
    SELECT pa
    FROM PostAnalysisEntity pa
    JOIN pa.contract c
    JOIN c.house h
    JOIN h.user u
    WHERE u.id = :userId
      AND pa.analysisType = :analysisType
    ORDER BY pa.createdAt DESC
    """)
    List<PostAnalysisEntity> findAllByUserIdAndAnalysisType(
            @Param("userId") Long userId,
            @Param("analysisType") AnalysisType analysisType
    );

    long countByHouse_Id(Long houseId);
    long countByHouse_IdAndAnalysisType(Long houseId, AnalysisType analysisType);
}
