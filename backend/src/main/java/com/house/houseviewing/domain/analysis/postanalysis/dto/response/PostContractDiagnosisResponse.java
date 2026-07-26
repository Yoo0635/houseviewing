package com.house.houseviewing.domain.analysis.postanalysis.dto.response;

import com.house.houseviewing.domain.analysis.postanalysis.entity.PostAnalysisEntity;
import com.house.houseviewing.domain.common.RiskLevel;
import com.house.houseviewing.domain.report.postreport.entity.PostReportEntity;
import com.house.houseviewing.global.exception.AppException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostContractDiagnosisResponse {

    private Long analysisId;

    private String nickname;

    private String address;

    private String mainReason;

    private RiskLevel riskLevel;

    private Integer ltvScore;

    private PdfGenerationStatus pdfStatus;

    private Long pdfReportId;

    private String filePath;

    private String pdfErrorCode;

    private String pdfErrorMessage;

    public static PostContractDiagnosisResponse success(PostAnalysisEntity analysis, PostReportEntity report) {
        return baseBuilder(analysis)
                .pdfStatus(PdfGenerationStatus.SUCCESS)
                .pdfReportId(report.getId())
                .filePath(report.getPdfPath())
                .build();
    }

    public static PostContractDiagnosisResponse pdfFailed(PostAnalysisEntity analysis, AppException e) {
        return baseBuilder(analysis)
                .pdfStatus(PdfGenerationStatus.FAILED)
                .pdfErrorCode(e.getExceptionCode().getCode())
                .pdfErrorMessage(e.getMessage())
                .build();
    }

    private static PostContractDiagnosisResponseBuilder baseBuilder(PostAnalysisEntity analysis) {
        return PostContractDiagnosisResponse.builder()
                .analysisId(analysis.getId())
                .nickname(analysis.getHouse().getNickname())
                .address(analysis.getHouse().getAddress().getAddressName())
                .mainReason(analysis.getMainReason())
                .riskLevel(analysis.getRiskLevel())
                .ltvScore(analysis.getLtvScore());
    }
}
