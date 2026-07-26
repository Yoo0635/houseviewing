package com.house.houseviewing.api.query;

import com.house.houseviewing.api.query.service.AnalysisQueryService;
import com.house.houseviewing.domain.analysis.postanalysis.dto.response.AnalysisResponse;
import com.house.houseviewing.domain.analysis.postanalysis.dto.response.PdfGenerationStatus;
import com.house.houseviewing.domain.analysis.postanalysis.dto.response.PostContractDiagnosisResponse;
import com.house.houseviewing.domain.analysis.postanalysis.entity.PostAnalysisEntity;
import com.house.houseviewing.domain.analysis.postanalysis.service.PostAnalysisService;
import com.house.houseviewing.domain.analysis.preanalysis.entity.PreAnalysisEntity;
import com.house.houseviewing.domain.analysis.preanalysis.service.PreAnalysisService;
import com.house.houseviewing.domain.analysis.preanalysis.dto.request.PreContractDiagnosisRequest;
import com.house.houseviewing.domain.report.postreport.entity.PostReportEntity;
import com.house.houseviewing.domain.report.postreport.service.PostReportService;
import com.house.houseviewing.domain.report.prereport.entity.PreReportEntity;
import com.house.houseviewing.domain.report.prereport.service.PreReportService;
import com.house.houseviewing.global.exception.AppException;
import com.house.houseviewing.global.exception.ExceptionCode;
import com.house.houseviewing.global.file.pdf.dto.PdfDownloadResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AnalysisQueryServiceTest {

    @InjectMocks AnalysisQueryService analysisQueryService;

    @Mock PostAnalysisService postAnalysisService;
    @Mock PreAnalysisService preAnalysisService;
    @Mock PostReportService postReportService;
    @Mock PreReportService preReportService;

    @Nested
    @DisplayName("사후 계약 진단 실행")
    class ExecutePostContractDiagnosis {

        @Test
        @DisplayName("성공")
        void 성공(){
            PostAnalysisEntity analysis = mock(PostAnalysisEntity.class, RETURNS_DEEP_STUBS);
            when(analysis.getId()).thenReturn(1L);
            when(analysis.getHouse().getNickname()).thenReturn("자취방");
            when(analysis.getHouse().getAddress().getAddressName()).thenReturn("서울시 강남구");
            when(analysis.getMainReason()).thenReturn("안전");
            when(analysis.getLtvScore()).thenReturn(82);
            PostReportEntity report = PostReportEntity.builder()
                    .id(11L)
                    .pdfPath("/test/path")
                    .build();

            given(postAnalysisService.postRegister(anyLong(), any(MultipartFile.class))).willReturn(analysis);
            given(postReportService.postRegister(any(PostAnalysisEntity.class))).willReturn(report);

            MultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", "data".getBytes());
            PostContractDiagnosisResponse result = analysisQueryService.executePostContractDiagnosis(1L, file);

            assertThat(result).isNotNull();
            assertThat(result.getFilePath()).isEqualTo("/test/path");
            assertThat(result.getPdfStatus()).isEqualTo(PdfGenerationStatus.SUCCESS);
            assertThat(result.getAnalysisId()).isEqualTo(1L);
            verify(postAnalysisService).postRegister(anyLong(), any(MultipartFile.class));
            verify(postReportService).postRegister(any(PostAnalysisEntity.class));
        }

        @Test
        @DisplayName("PDF 실패여도 분석 성공 응답 반환")
        void pdf_실패_분리(){
            PostAnalysisEntity analysis = mock(PostAnalysisEntity.class, RETURNS_DEEP_STUBS);
            when(analysis.getId()).thenReturn(1L);
            when(analysis.getHouse().getNickname()).thenReturn("자취방");
            when(analysis.getHouse().getAddress().getAddressName()).thenReturn("서울시 강남구");
            when(analysis.getMainReason()).thenReturn("안전");
            when(analysis.getLtvScore()).thenReturn(82);

            given(postAnalysisService.postRegister(anyLong(), any(MultipartFile.class))).willReturn(analysis);
            given(postReportService.postRegister(any(PostAnalysisEntity.class)))
                    .willThrow(new AppException(ExceptionCode.INVALID_PDF_REQUEST, "contractType 필드는 필수입니다."));

            MultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", "data".getBytes());
            PostContractDiagnosisResponse result = analysisQueryService.executePostContractDiagnosis(1L, file);

            assertThat(result.getAnalysisId()).isEqualTo(1L);
            assertThat(result.getLtvScore()).isEqualTo(82);
            assertThat(result.getPdfStatus()).isEqualTo(PdfGenerationStatus.FAILED);
            assertThat(result.getPdfErrorCode()).isEqualTo(ExceptionCode.INVALID_PDF_REQUEST.getCode());
            assertThat(result.getPdfErrorMessage()).contains("contractType");
        }
    }

    @Nested
    @DisplayName("사전 계약 진단 실행")
    class ExecutePreContractDiagnosis {

        @Test
        @DisplayName("성공")
        void 성공(){
            PreAnalysisEntity analysis = mock(PreAnalysisEntity.class);
            when(analysis.getId()).thenReturn(1L);
            PreReportEntity report = PreReportEntity.builder()
                    .pdfPath("/pre/path")
                    .build();

            given(preAnalysisService.preRegister(anyLong(), any(PreContractDiagnosisRequest.class), any(MultipartFile.class)))
                    .willReturn(analysis);
            given(preReportService.preRegister(any(PreAnalysisEntity.class))).willReturn(report);

            PreContractDiagnosisRequest request = PreContractDiagnosisRequest.builder()
                    .nickname("테스트")
                    .address("서울")
                    .build();
            MultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", "data".getBytes());
            PdfDownloadResponse result = analysisQueryService.executePreContractDiagnosis(1L, request, file);

            assertThat(result).isNotNull();
            assertThat(result.getFilePath()).isEqualTo("/pre/path");
        }
    }

    @Nested
    @DisplayName("분석 목록 조회")
    class GetAnalyses {

        @Test
        @DisplayName("성공")
        void 성공(){
            AnalysisResponse postResponse = AnalysisResponse.builder()
                    .pdfReportId(11L)
                    .nickname("사후")
                    .mainReason("안전")
                    .build();
            AnalysisResponse preResponse = AnalysisResponse.builder()
                    .pdfReportId(22L)
                    .nickname("사전")
                    .mainReason("주의")
                    .build();

            given(postAnalysisService.getPostAnalyses(anyLong())).willReturn(List.of(postResponse));
            given(preAnalysisService.getPreAnalyses(anyLong())).willReturn(List.of(preResponse));

            List<AnalysisResponse> result = analysisQueryService.getAnalyses(1L);

            assertThat(result).hasSize(2);
            assertThat(result.get(0).getPdfReportId()).isEqualTo(11L);
            assertThat(result.get(0).getNickname()).isEqualTo("사후");
            assertThat(result.get(1).getPdfReportId()).isEqualTo(22L);
            assertThat(result.get(1).getNickname()).isEqualTo("사전");
        }
    }

    @Nested
    @DisplayName("차이 분석 목록 조회")
    class GetDiffAnalyses {

        @Test
        @DisplayName("성공")
        void 성공(){
            AnalysisResponse diffResponse = AnalysisResponse.builder()
                    .pdfReportId(33L)
                    .nickname("차이")
                    .mainReason("주의")
                    .build();

            given(postAnalysisService.getDiffAnalyses(anyLong())).willReturn(List.of(diffResponse));

            List<AnalysisResponse> result = analysisQueryService.getDiffAnalyses(1L);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getPdfReportId()).isEqualTo(33L);
            assertThat(result.get(0).getNickname()).isEqualTo("차이");
        }
    }
}
