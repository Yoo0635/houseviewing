package com.house.houseviewing.domain.report.prereport;

import com.house.houseviewing.domain.analysis.preanalysis.entity.PreAnalysisEntity;
import com.house.houseviewing.domain.report.prereport.entity.PreReportEntity;
import com.house.houseviewing.fixture.AddressFixture;
import com.house.houseviewing.fixture.PreAnalysisFixture;
import com.house.houseviewing.fixture.PreReportFixture;
import com.house.houseviewing.fixture.UserFixture;
import com.house.houseviewing.domain.user.entity.UserEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class PreReportEntityTest {

    @Test
    @DisplayName("분석 추가")
    void 분석_추가(){
        UserEntity user = UserFixture.createDefaultWithId(1L);
        PreAnalysisEntity analysis = PreAnalysisFixture.createWithId(user, 1L);
        PreReportEntity report = PreReportFixture.createDefault().build();

        report.addAnalysis(analysis);

        assertThat(report.getAnalysis()).isEqualTo(analysis);
        assertThat(analysis.getPreReportEntity()).isEqualTo(report);
    }

    @Test
    @DisplayName("빌더 패턴 테스트")
    void 빌더_패턴(){
        PreReportEntity report = PreReportEntity.builder()
                .pdfKey("key-123")
                .pdfPath("/path/to/file")
                .pdfSizeBytes(5000L)
                .pdfName("report.pdf")
                .build();

        assertThat(report.getPdfKey()).isEqualTo("key-123");
        assertThat(report.getPdfPath()).isEqualTo("/path/to/file");
        assertThat(report.getPdfSizeBytes()).isEqualTo(5000L);
        assertThat(report.getPdfName()).isEqualTo("report.pdf");
    }
}
