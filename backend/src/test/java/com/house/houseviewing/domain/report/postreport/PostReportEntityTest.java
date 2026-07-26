package com.house.houseviewing.domain.report.postreport;

import com.house.houseviewing.domain.analysis.postanalysis.entity.PostAnalysisEntity;
import com.house.houseviewing.domain.common.RiskLevel;
import com.house.houseviewing.domain.house.entity.HouseEntity;
import com.house.houseviewing.domain.report.postreport.entity.PostReportEntity;
import com.house.houseviewing.fixture.HouseFixture;
import com.house.houseviewing.fixture.PostAnalysisFixture;
import com.house.houseviewing.fixture.PostReportFixture;
import com.house.houseviewing.fixture.UserFixture;
import com.house.houseviewing.domain.user.entity.UserEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class PostReportEntityTest {

    @Test
    @DisplayName("분석 추가")
    void 분석_추가(){
        UserEntity user = UserFixture.createDefaultWithId(1L);
        HouseEntity house = HouseFixture.createWithUserAndId(user, 1L);
        PostAnalysisEntity analysis = PostAnalysisFixture.createWithId(house, 1L);
        PostReportEntity report = PostReportFixture.createDefault().build();

        report.addRegistryAnalysis(analysis);

        assertThat(report.getAnalysis()).isEqualTo(analysis);
        assertThat(analysis.getPdfReport()).isEqualTo(report);
    }

    @Test
    @DisplayName("빌더 패턴 테스트")
    void 빌더_패턴(){
        PostReportEntity report = PostReportEntity.builder()
                .pdfKey("post-key-456")
                .pdfPath("/post/path/file")
                .pdfSizeBytes(10000L)
                .pdfName("post_report.pdf")
                .build();

        assertThat(report.getPdfKey()).isEqualTo("post-key-456");
        assertThat(report.getPdfPath()).isEqualTo("/post/path/file");
        assertThat(report.getPdfSizeBytes()).isEqualTo(10000L);
        assertThat(report.getPdfName()).isEqualTo("post_report.pdf");
    }
}
