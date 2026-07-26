package com.house.houseviewing.global.file.diff.dto;

import com.house.houseviewing.domain.common.RiskLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@Builder
@Getter
@NoArgsConstructor
public class DiffAnalysisResult {

    private RiskLevel riskLevel;

    private String rawData;

    private String mainReason;

    private Integer ltvScore;
}
