package com.house.houseviewing.api.query;

import com.house.houseviewing.api.query.service.DiffAnalysisQueryService;
import com.house.houseviewing.domain.analysis.postanalysis.entity.PostAnalysisEntity;
import com.house.houseviewing.domain.analysis.postanalysis.repository.PostAnalysisRepository;
import com.house.houseviewing.domain.analysis.postanalysis.service.PostAnalysisService;
import com.house.houseviewing.domain.report.postreport.entity.PostReportEntity;
import com.house.houseviewing.domain.report.postreport.service.PostReportService;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DiffAnalysisQueryServiceTest {

    @InjectMocks DiffAnalysisQueryService diffAnalysisQueryService;

    @Mock PostAnalysisRepository postAnalysisRepository;
    @Mock PostAnalysisService postAnalysisService;
    @Mock PostReportService postReportService;

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

            given(postAnalysisRepository.countByHouse_Id(anyLong())).willReturn(1L);
            given(postAnalysisService.diffRegister(anyLong(), anyString())).willReturn(analysis);
            given(postReportService.diffRegister(any(PostAnalysisEntity.class))).willReturn(report);

            PdfDownloadResponse result = diffAnalysisQueryService.executeDiffDiagnosis(1L);

            assertThat(result).isNotNull();
            assertThat(result.getFilePath()).isEqualTo("/diff/path");
            verify(postAnalysisService).diffRegister(anyLong(), anyString());
            verify(postReportService).diffRegister(any(PostAnalysisEntity.class));
        }

        @Test
        @DisplayName("count가 0일 때 SAFE 레지스트리 사용")
        void count_0(){
            PostAnalysisEntity analysis = mock(PostAnalysisEntity.class);
            when(analysis.getId()).thenReturn(1L);
            PostReportEntity report = PostReportEntity.builder()
                    .pdfPath("/diff/path")
                    .build();

            given(postAnalysisRepository.countByHouse_Id(anyLong())).willReturn(0L);
            given(postAnalysisService.diffRegister(anyLong(), anyString())).willReturn(analysis);
            given(postReportService.diffRegister(any(PostAnalysisEntity.class))).willReturn(report);

            diffAnalysisQueryService.executeDiffDiagnosis(1L);

            verify(postAnalysisService).diffRegister(eq(1L), contains("\"status\": \"말소\""));
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
