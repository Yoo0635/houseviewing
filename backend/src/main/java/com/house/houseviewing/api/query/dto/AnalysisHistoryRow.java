package com.house.houseviewing.api.query.dto;

import com.house.houseviewing.domain.analysis.postanalysis.dto.response.AnalysisResponse;
import com.house.houseviewing.domain.common.RiskLevel;

import java.time.LocalDateTime;

public record AnalysisHistoryRow(
        Long analysisId,
        LocalDateTime createdAt,
        String analysisType,
        Long pdfReportId,
        String nickname,
        String address,
        String mainReason,
        RiskLevel riskLevel,
        Integer ltvScore
) {

    public AnalysisResponse toResponse() {
        return AnalysisResponse.builder()
                .analysisId(analysisId)
                .createdAt(createdAt)
                .pdfReportId(pdfReportId)
                .nickname(nickname)
                .address(address)
                .mainReason(mainReason)
                .riskLevel(riskLevel)
                .ltvScore(ltvScore)
                .analysisType(analysisType)
                .build();
    }
}
