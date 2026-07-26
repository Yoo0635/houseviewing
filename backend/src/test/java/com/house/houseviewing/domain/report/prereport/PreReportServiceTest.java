package com.house.houseviewing.domain.report.prereport;

import com.house.houseviewing.domain.analysis.preanalysis.entity.PreAnalysisEntity;
import com.house.houseviewing.domain.report.prereport.entity.PreReportEntity;
import com.house.houseviewing.domain.report.prereport.repository.PreReportRepository;
import com.house.houseviewing.domain.report.prereport.service.PreReportService;
import com.house.houseviewing.domain.user.entity.UserEntity;
import com.house.houseviewing.fixture.AddressFixture;
import com.house.houseviewing.fixture.PreAnalysisFixture;
import com.house.houseviewing.fixture.UserFixture;
import com.house.houseviewing.global.file.pdf.dto.PdfUploadResult;
import com.house.houseviewing.global.file.pdf.service.PdfReportTransferAndReceiveService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class PreReportServiceTest {

    @InjectMocks PreReportService preReportService;

    @Mock PreReportRepository preReportRepository;
    @Mock PdfReportTransferAndReceiveService pdfReportTransferAndReceiveService;

    @Nested
    @DisplayName("사전 리포트 등록")
    class PreRegister {

        @Test
        @DisplayName("성공")
        void 성공(){
            UserEntity user = UserFixture.createDefaultWithId(1L);
            PreAnalysisEntity analysis = PreAnalysisFixture.createWithId(user, 1L);
            PdfUploadResult uploadResult = PdfUploadResult.builder()
                    .pdfKey("test-key")
                    .pdfPath("/test/path")
                    .pdfName("test.pdf")
                    .pdfSizeBytes(1024L)
                    .build();

            given(pdfReportTransferAndReceiveService.preTransferAndReceive(any())).willReturn(uploadResult);
            given(preReportRepository.save(any(PreReportEntity.class))).willAnswer(invocation -> {
                PreReportEntity entity = invocation.getArgument(0);
                entity.getClass().getDeclaredField("id");
                return entity;
            });

            PreReportEntity result = preReportService.preRegister(analysis);

            assertThat(result).isNotNull();
            assertThat(result.getPdfKey()).isEqualTo("test-key");
            assertThat(result.getPdfPath()).isEqualTo("/test/path");
            assertThat(result.getPdfName()).isEqualTo("test.pdf");
            assertThat(result.getPdfSizeBytes()).isEqualTo(1024L);
            verify(preReportRepository).save(any(PreReportEntity.class));
        }
    }
}
