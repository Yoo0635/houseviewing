package com.house.houseviewing.api.query;

import com.house.houseviewing.api.query.dto.AnalysisHistoryPageResponse;
import com.house.houseviewing.api.query.dto.AnalysisHistoryRow;
import com.house.houseviewing.api.query.repository.AnalysisHistoryQueryRepository;
import com.house.houseviewing.api.query.service.AnalysisQueryService;
import com.house.houseviewing.domain.analysis.postanalysis.dto.response.AnalysisResponse;
import com.house.houseviewing.domain.analysis.postanalysis.dto.response.PdfGenerationStatus;
import com.house.houseviewing.domain.analysis.postanalysis.dto.response.PostContractDiagnosisResponse;
import com.house.houseviewing.domain.analysis.postanalysis.entity.PostAnalysisEntity;
import com.house.houseviewing.domain.analysis.postanalysis.service.PostAnalysisService;
import com.house.houseviewing.domain.analysis.preanalysis.entity.PreAnalysisEntity;
import com.house.houseviewing.domain.analysis.preanalysis.service.PreAnalysisService;
import com.house.houseviewing.domain.analysis.preanalysis.dto.request.PreContractDiagnosisRequest;
import com.house.houseviewing.domain.common.Address;
import com.house.houseviewing.domain.common.RiskLevel;
import com.house.houseviewing.domain.report.postreport.entity.PostReportEntity;
import com.house.houseviewing.domain.report.postreport.service.PostReportRetryService;
import com.house.houseviewing.domain.report.postreport.service.PostReportService;
import com.house.houseviewing.domain.report.prereport.entity.PreReportEntity;
import com.house.houseviewing.domain.report.prereport.service.PreReportService;
import com.house.houseviewing.domain.subscription.enums.FreeDiagnosisStage;
import com.house.houseviewing.domain.subscription.enums.FreeDiagnosisStatus;
import com.house.houseviewing.domain.subscription.service.FreeDiagnosisClaim;
import com.house.houseviewing.domain.subscription.service.FreeDiagnosisPersistenceService;
import com.house.houseviewing.global.exception.AppException;
import com.house.houseviewing.global.exception.ExceptionCode;
import com.house.houseviewing.global.file.pdf.dto.PdfDownloadResponse;
import com.house.houseviewing.global.file.pdf.dto.PdfUploadResult;
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

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
    @Mock PostReportRetryService postReportRetryService;
    @Mock PreReportService preReportService;
    @Mock FreeDiagnosisPersistenceService freeDiagnosisPersistenceService;
    @Mock AnalysisHistoryQueryRepository analysisHistoryQueryRepository;

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
            verify(postReportRetryService).enqueue(eq(analysis), any(AppException.class));
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
                    .id(10L)
                    .pdfPath("/pre/path")
                    .build();
            PdfUploadResult uploadResult = PdfUploadResult.builder()
                    .pdfPath("/pre/path")
                    .pdfName("pre.pdf")
                    .pdfKey("pre-key")
                    .pdfSizeBytes(100L)
                    .build();
            Address address = mock(Address.class);
            String requestId = "7c7f9b06-f096-48eb-bfa7-09cbe9d1bc93";

            given(freeDiagnosisPersistenceService.claim(1L, requestId))
                    .willReturn(new FreeDiagnosisClaim(true, false, FreeDiagnosisStatus.PROCESSING, FreeDiagnosisStage.ADDRESS));
            given(preAnalysisService.parseAddress(anyString())).willReturn(address);
            given(preAnalysisService.analyze(any(PreContractDiagnosisRequest.class), eq(address), any(MultipartFile.class)))
                    .willReturn(analysis);
            given(preAnalysisService.save(1L, requestId, analysis)).willReturn(analysis);
            given(preReportService.createPdf(analysis)).willReturn(uploadResult);
            given(preReportService.save(analysis, uploadResult)).willReturn(report);

            PreContractDiagnosisRequest request = PreContractDiagnosisRequest.builder()
                    .nickname("테스트")
                    .address("서울")
                    .build();
            MultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", "data".getBytes());
            PdfDownloadResponse result = analysisQueryService.executePreContractDiagnosis(1L, requestId, request, file);

            assertThat(result).isNotNull();
            assertThat(result.getPdfReportId()).isEqualTo(10L);
            assertThat(result.getFilePath()).isEqualTo("/pre/path");
            assertThat(result.getStatus()).isEqualTo("COMPLETED");
            assertThat(result.getRequestId()).isEqualTo(requestId);
            verify(freeDiagnosisPersistenceService).updateStage(1L, requestId, FreeDiagnosisStage.ANALYSIS);
            verify(freeDiagnosisPersistenceService).updateStage(1L, requestId, FreeDiagnosisStage.PDF);
            verify(freeDiagnosisPersistenceService).complete(1L, requestId);
        }

        @Test
        @DisplayName("같은 요청이 처리 중이면 외부 호출 없이 상태를 반환")
        void 같은_요청_처리중(){
            String requestId = "7c7f9b06-f096-48eb-bfa7-09cbe9d1bc93";
            given(freeDiagnosisPersistenceService.claim(1L, requestId))
                    .willReturn(new FreeDiagnosisClaim(false, false, FreeDiagnosisStatus.PROCESSING, FreeDiagnosisStage.PDF));

            PreContractDiagnosisRequest request = PreContractDiagnosisRequest.builder()
                    .nickname("테스트")
                    .address("서울")
                    .build();
            MultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", "data".getBytes());
            PdfDownloadResponse result = analysisQueryService.executePreContractDiagnosis(1L, requestId, request, file);

            assertThat(result.getPdfReportId()).isNull();
            assertThat(result.getStatus()).isEqualTo("PROCESSING");
            assertThat(result.getStage()).isEqualTo("PDF");
            assertThat(result.getRequestId()).isEqualTo(requestId);
            verify(preAnalysisService, never()).analyze(any(), any(), any());
            verify(preReportService, never()).createPdf(any());
        }
    }

    @Nested
    @DisplayName("분석 목록 조회")
    class GetAnalyses {

        @Test
        @DisplayName("첫 페이지 10건과 다음 offset을 반환한다")
        void 첫_페이지(){
            given(analysisHistoryQueryRepository.findPostAnalyses(1L, null, 0L, 11L))
                    .willReturn(rows("POST", 11, 0));
            given(analysisHistoryQueryRepository.findPreAnalyses(1L, null, 0L, 11L))
                    .willReturn(List.of());

            AnalysisHistoryPageResponse result = analysisQueryService.getAnalyses(1L, 0L, null);

            assertThat(result.items()).hasSize(10);
            assertThat(result.nextOffset()).isEqualTo(10L);
            assertThat(result.hasNext()).isTrue();
            assertThat(result.items().get(0).getAnalysisId()).isEqualTo(100L);
        }

        @Test
        @DisplayName("마지막 페이지는 다음 offset을 null로 반환한다")
        void 마지막_페이지(){
            given(analysisHistoryQueryRepository.findPostAnalyses(1L, null, 0L, 16L))
                    .willReturn(rows("POST", 4, 20));
            given(analysisHistoryQueryRepository.findPreAnalyses(1L, null, 0L, 16L))
                    .willReturn(rows("PRE", 2, 24));

            AnalysisHistoryPageResponse result = analysisQueryService.getAnalyses(1L, 5L, null);

            assertThat(result.items()).hasSize(1);
            assertThat(result.nextOffset()).isNull();
            assertThat(result.hasNext()).isFalse();
        }

        @Test
        @DisplayName("PRE와 POST를 정렬한 뒤 통합 offset을 한 번만 적용한다")
        void 통합_정렬_후_offset(){
            given(analysisHistoryQueryRepository.findPostAnalyses(1L, null, 0L, 13L))
                    .willReturn(List.of(
                            row(10L, "POST", 0, RiskLevel.SAFE),
                            row(8L, "POST", 1, RiskLevel.SAFE),
                            row(7L, "POST", 1, RiskLevel.SAFE)
                    ));
            given(analysisHistoryQueryRepository.findPreAnalyses(1L, null, 0L, 13L))
                    .willReturn(List.of(
                            row(9L, "PRE", 0, RiskLevel.SAFE),
                            row(6L, "PRE", 2, RiskLevel.SAFE)
                    ));

            AnalysisHistoryPageResponse result = analysisQueryService.getAnalyses(1L, 2L, null);

            assertThat(result.items())
                    .extracting(AnalysisResponse::getAnalysisId)
                    .containsExactly(8L, 7L, 6L);
            assertThat(result.hasNext()).isFalse();
        }

        @Test
        @DisplayName("위험도 필터를 페이징 전에 적용한다")
        void 위험도_필터(){
            given(analysisHistoryQueryRepository.findPostAnalyses(1L, RiskLevel.DANGER, 0L, 11L))
                    .willReturn(List.of(row(1L, "POST", 0, RiskLevel.DANGER)));
            given(analysisHistoryQueryRepository.findPreAnalyses(1L, RiskLevel.DANGER, 0L, 11L))
                    .willReturn(List.of());

            AnalysisHistoryPageResponse result = analysisQueryService.getAnalyses(1L, 0L, RiskLevel.DANGER);

            assertThat(result.items()).hasSize(1);
            assertThat(result.items().get(0).getRiskLevel()).isEqualTo(RiskLevel.DANGER);
            then(analysisHistoryQueryRepository).should().findPostAnalyses(1L, RiskLevel.DANGER, 0L, 11L);
            then(analysisHistoryQueryRepository).should().findPreAnalyses(1L, RiskLevel.DANGER, 0L, 11L);
        }

        @Test
        @DisplayName("지원하지 않는 offset은 거절한다")
        void offset_검증(){
            assertThatThrownBy(() -> analysisQueryService.getAnalyses(1L, -1L, null))
                    .isInstanceOf(AppException.class);
            assertThatThrownBy(() -> analysisQueryService.getAnalyses(1L, 10_001L, null))
                    .isInstanceOf(AppException.class);
        }
    }

    @Nested
    @DisplayName("차이 분석 목록 조회")
    class GetDiffAnalyses {

        @Test
        @DisplayName("DIFF만 조회하고 offset을 적용한다")
        void diff_only(){
            given(analysisHistoryQueryRepository.findDiffAnalyses(1L, RiskLevel.WARNING, 3L, 11L))
                    .willReturn(rows("POST", 3, 30));

            AnalysisHistoryPageResponse result = analysisQueryService.getDiffAnalyses(1L, 3L, RiskLevel.WARNING);

            assertThat(result.items()).hasSize(3);
            assertThat(result.nextOffset()).isNull();
            assertThat(result.hasNext()).isFalse();
            then(analysisHistoryQueryRepository).should().findDiffAnalyses(1L, RiskLevel.WARNING, 3L, 11L);
        }
    }

    private List<AnalysisHistoryRow> rows(String type, int size, int minutesAgoStart) {
        return java.util.stream.IntStream.range(0, size)
                .mapToObj(i -> row(100L + i, type, minutesAgoStart + i, RiskLevel.SAFE))
                .toList();
    }

    private AnalysisHistoryRow row(Long id, String type, int minutesAgo, RiskLevel riskLevel) {
        return new AnalysisHistoryRow(
                id,
                LocalDateTime.of(2026, 9, 14, 12, 0).minusMinutes(minutesAgo),
                type,
                id + 1000,
                type + "집",
                "서울",
                "이유",
                riskLevel,
                80
        );
    }
}
