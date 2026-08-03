package com.house.houseviewing.api.query;

import com.house.houseviewing.api.query.service.DiffAnalysisQueryService;
import com.house.houseviewing.api.query.service.DiffDiagnosisLockService;
import com.house.houseviewing.domain.analysis.postanalysis.entity.PostAnalysisEntity;
import com.house.houseviewing.domain.analysis.postanalysis.enums.AnalysisType;
import com.house.houseviewing.domain.analysis.postanalysis.repository.PostAnalysisRepository;
import com.house.houseviewing.domain.analysis.postanalysis.service.PostAnalysisService;
import com.house.houseviewing.domain.report.postreport.entity.PostReportEntity;
import com.house.houseviewing.domain.report.postreport.service.PostReportService;
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

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DiffAnalysisQueryServiceTest {

    @InjectMocks DiffAnalysisQueryService diffAnalysisQueryService;

    @Mock PostAnalysisRepository postAnalysisRepository;
    @Mock PostAnalysisService postAnalysisService;
    @Mock PostReportService postReportService;
    @Mock DiffDiagnosisLockService diffDiagnosisLockService;

    @Nested
    @DisplayName("차이 진단 실행")
    class ExecuteDiffDiagnosis {

        @Test
        @DisplayName("성공")
        void 성공(){
            PostAnalysisEntity analysis = mock(PostAnalysisEntity.class);
            when(analysis.getId()).thenReturn(1L);
            PostReportEntity report = PostReportEntity.builder()
                    .pdfPath("/diff/path")
                    .build();

            given(postAnalysisRepository.countByHouse_IdAndAnalysisType(anyLong(), eq(AnalysisType.DIFF))).willReturn(1L);
            given(postAnalysisRepository.findByHouse_IdAndAnalysisTypeAndSnapshotHash(anyLong(), eq(AnalysisType.DIFF), anyString()))
                    .willReturn(Optional.empty());
            given(diffDiagnosisLockService.createSnapshotHash(anyString())).willReturn("hash-1");
            given(diffDiagnosisLockService.tryLock(anyLong(), anyString())).willReturn("token-1");
            given(postAnalysisService.diffRegister(anyLong(), anyString(), anyString())).willReturn(analysis);
            given(postReportService.diffRegister(any(PostAnalysisEntity.class))).willReturn(report);

            PdfDownloadResponse result = diffAnalysisQueryService.executeDiffDiagnosis(1L);

            assertThat(result).isNotNull();
            assertThat(result.getFilePath()).isEqualTo("/diff/path");
            verify(postAnalysisService).diffRegister(anyLong(), anyString(), eq("hash-1"));
            verify(postReportService).diffRegister(any(PostAnalysisEntity.class));
            verify(diffDiagnosisLockService).release(1L, "hash-1", "token-1");
        }

        @Test
        @DisplayName("count가 0일 때 SAFE 레지스트리 사용")
        void count_0(){
            PostAnalysisEntity analysis = mock(PostAnalysisEntity.class);
            when(analysis.getId()).thenReturn(1L);
            PostReportEntity report = PostReportEntity.builder()
                    .pdfPath("/diff/path")
                    .build();

            given(postAnalysisRepository.countByHouse_IdAndAnalysisType(anyLong(), eq(AnalysisType.DIFF))).willReturn(0L);
            given(postAnalysisRepository.findByHouse_IdAndAnalysisTypeAndSnapshotHash(anyLong(), eq(AnalysisType.DIFF), anyString()))
                    .willReturn(Optional.empty());
            given(diffDiagnosisLockService.createSnapshotHash(anyString())).willReturn("hash-safe");
            given(diffDiagnosisLockService.tryLock(anyLong(), anyString())).willReturn("token-safe");
            given(postAnalysisService.diffRegister(anyLong(), anyString(), anyString())).willReturn(analysis);
            given(postReportService.diffRegister(any(PostAnalysisEntity.class))).willReturn(report);

            diffAnalysisQueryService.executeDiffDiagnosis(1L);

            verify(postAnalysisService).diffRegister(eq(1L), contains("\"status\": \"말소\""), eq("hash-safe"));
        }

        @Test
        @DisplayName("동일 스냅샷 처리 중이면 분석과 PDF 생성을 실행하지 않음")
        void same_snapshot_in_progress(){
            given(postAnalysisRepository.countByHouse_IdAndAnalysisType(anyLong(), eq(AnalysisType.DIFF))).willReturn(0L);
            given(postAnalysisRepository.findByHouse_IdAndAnalysisTypeAndSnapshotHash(anyLong(), eq(AnalysisType.DIFF), anyString()))
                    .willReturn(Optional.empty());
            given(diffDiagnosisLockService.createSnapshotHash(anyString())).willReturn("same-hash");
            given(diffDiagnosisLockService.tryLock(anyLong(), eq("same-hash"))).willReturn(null);

            assertThatThrownBy(() -> diffAnalysisQueryService.executeDiffDiagnosis(1L))
                    .isInstanceOf(AppException.class)
                    .extracting("exceptionCode")
                    .isEqualTo(ExceptionCode.DIFF_DIAGNOSIS_IN_PROGRESS);

            verify(postAnalysisService, never()).diffRegister(anyLong(), anyString(), anyString());
            verify(postReportService, never()).diffRegister(any(PostAnalysisEntity.class));
        }

        @Test
        @DisplayName("이미 같은 스냅샷 분석과 PDF가 있으면 기존 PDF 응답을 재사용")
        void reuse_existing_snapshot_result(){
            PostReportEntity report = PostReportEntity.builder()
                    .id(10L)
                    .pdfPath("/diff/existing")
                    .build();
            PostAnalysisEntity analysis = mock(PostAnalysisEntity.class);
            given(analysis.getPdfReport()).willReturn(report);
            given(postAnalysisRepository.countByHouse_IdAndAnalysisType(anyLong(), eq(AnalysisType.DIFF))).willReturn(0L);
            given(diffDiagnosisLockService.createSnapshotHash(anyString())).willReturn("same-hash");
            given(postAnalysisRepository.findByHouse_IdAndAnalysisTypeAndSnapshotHash(1L, AnalysisType.DIFF, "same-hash"))
                    .willReturn(Optional.of(analysis));

            PdfDownloadResponse result = diffAnalysisQueryService.executeDiffDiagnosis(1L);

            assertThat(result.getFilePath()).isEqualTo("/diff/existing");
            verify(diffDiagnosisLockService, never()).tryLock(anyLong(), anyString());
            verify(postAnalysisService, never()).diffRegister(anyLong(), anyString(), anyString());
            verify(postReportService, never()).diffRegister(any(PostAnalysisEntity.class));
        }
    }

    @Nested
    @DisplayName("Mock JSON 읽기")
    class ReadMockJson {

        @Test
        @DisplayName("count 0: SAFE 파일")
        void count_0(){
            String result = diffAnalysisQueryService.readMockJson(0);
            assertThat(result)
                    .contains("\"status\": \"말소\"")
                    .contains("\"name\": \"유인근\"");
        }

        @Test
        @DisplayName("count 1: WARNING 파일")
        void count_1(){
            String result = diffAnalysisQueryService.readMockJson(1);
            assertThat(result)
                    .contains("\"date\": \"2025-12-15\"")
                    .contains("\"max_claim_amount\": 120000000");
        }

        @Test
        @DisplayName("count 2 이상: DANGER 파일")
        void count_2(){
            String result = diffAnalysisQueryService.readMockJson(2);
            assertThat(result)
                    .contains("\"purpose\": \"가압류\"")
                    .contains("\"description\": \"채권자 국민건강보험공단\"");
        }
    }
}
