package com.house.houseviewing.domain.analysis.postanalysis.dto.response;

import com.house.houseviewing.domain.analysis.preanalysis.entity.PreAnalysisEntity;
import com.house.houseviewing.domain.common.RiskLevel;
import com.house.houseviewing.domain.analysis.postanalysis.entity.PostAnalysisEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AnalysisResponse {

    private Long pdfReportId;

    private String nickname;

    private String address;

    private String mainReason;

    private RiskLevel riskLevel;

    private Integer ltvScore;

    private String analysisType;

    public static AnalysisResponse from(PostAnalysisEntity postAnalysis){
        String nickname = postAnalysis.getHouse().getNickname();
        String address = postAnalysis.getHouse().getAddress().getAddressName();
        return AnalysisResponse.builder()
                .pdfReportId(postAnalysis.getPdfReport() != null ? postAnalysis.getPdfReport().getId() : null)
                .nickname(nickname)
                .address(address)
                .mainReason(postAnalysis.getMainReason())
                .riskLevel(postAnalysis.getRiskLevel())
                .ltvScore(postAnalysis.getLtvScore())
                .analysisType("POST")
                .build();
    }

    public static AnalysisResponse from(PreAnalysisEntity preAnalysis){
        return AnalysisResponse.builder()
                .pdfReportId(preAnalysis.getPreReportEntity() != null ? preAnalysis.getPreReportEntity().getId() : null)
                .nickname(preAnalysis.getNickname())
                .address(preAnalysis.getAddress().getAddressName())
                .mainReason(preAnalysis.getMainReason())
                .riskLevel(preAnalysis.getRiskLevel())
                .ltvScore(preAnalysis.getLtvScore())
                .analysisType("PRE")
                .build();
    }
}
