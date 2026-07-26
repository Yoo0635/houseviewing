package com.house.houseviewing.domain.analysis.postanalysis;

import com.house.houseviewing.domain.analysis.postanalysis.dto.response.AnalysisResponse;
import com.house.houseviewing.domain.analysis.preanalysis.entity.PreAnalysisEntity;
import com.house.houseviewing.domain.common.Address;
import com.house.houseviewing.domain.common.RiskLevel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * AnalysisResponse.analysisType 필드 검증
 * - PRE/POST 테이블 pdfReportId 충돌 문제 해결을 위해 추가된 필드
 * - from(PreAnalysisEntity) → "PRE"
 * - from(PostAnalysisEntity) → "POST"
 */
class AnalysisResponseAnalysisTypeTest {

    @Test
    @DisplayName("PreAnalysisEntity → analysisType = PRE")
    void pre_analysis_returns_PRE_type() {
        PreAnalysisEntity pre = PreAnalysisEntity.builder()
                .nickname("무료 1회 진단")
                .rawData("{}")
                .mainReason("특이사항 없음")
                .address(Address.builder()
                        .addressName("경기도 오산시 양산동 387")
                        .region1DepthName("경기도")
                        .region2DepthName("오산시")
                        .region3DepthName("양산동")
                        .mainAddressNo("387")
                        .subAddressNo("")
                        .detailAddress("")
                        .build())
                .riskLevel(RiskLevel.SAFE)
                .ltvScore(27)
                .build();

        AnalysisResponse response = AnalysisResponse.from(pre);

        assertThat(response.getAnalysisType()).isEqualTo("PRE");
        assertThat(response.getLtvScore()).isEqualTo(27);
        assertThat(response.getNickname()).isEqualTo("무료 1회 진단");
    }

    @Test
    @DisplayName("PRE 타입 필터 적용 시 POST(0점) 제외 확인")
    void filter_by_PRE_excludes_POST_zero_score() {
        // PRE 레코드 (ltvScore=27)
        AnalysisResponse preResponse = AnalysisResponse.builder()
                .pdfReportId(1L)
                .nickname("무료 1회 진단")
                .address("경기도 오산시 양산동 387")
                .ltvScore(27)
                .riskLevel(RiskLevel.SAFE)
                .analysisType("PRE")
                .build();

        // POST 레코드 (ltvScore=0, pdfReportId 충돌)
        AnalysisResponse postResponse = AnalysisResponse.builder()
                .pdfReportId(1L)
                .nickname("에덴하우스")
                .address("경기도 오산시 양산동 387")
                .ltvScore(0)
                .riskLevel(RiskLevel.SAFE)
                .analysisType("POST")
                .build();

        java.util.List<AnalysisResponse> combined = java.util.List.of(postResponse, preResponse);

        // 앱과 동일한 필터 로직: analysisType == "PRE" 만 추출
        java.util.List<AnalysisResponse> preOnly = combined.stream()
                .filter(r -> "PRE".equals(r.getAnalysisType()) || r.getAnalysisType() == null)
                .toList();

        assertThat(preOnly).hasSize(1);
        assertThat(preOnly.get(0).getLtvScore()).isEqualTo(27);
        assertThat(preOnly.get(0).getAnalysisType()).isEqualTo("PRE");
    }
}
