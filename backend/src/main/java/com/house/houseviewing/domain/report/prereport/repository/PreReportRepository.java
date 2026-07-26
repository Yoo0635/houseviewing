package com.house.houseviewing.domain.report.prereport.repository;

import com.house.houseviewing.domain.report.prereport.entity.PreReportEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PreReportRepository extends JpaRepository<PreReportEntity, Long> {
}
