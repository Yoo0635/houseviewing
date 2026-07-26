package com.house.houseviewing.domain.analysis.preanalysis;

import com.house.houseviewing.domain.analysis.preanalysis.entity.PreAnalysisEntity;
import com.house.houseviewing.domain.common.RiskLevel;
import com.house.houseviewing.domain.report.prereport.entity.PreReportEntity;
import com.house.houseviewing.fixture.AddressFixture;
import com.house.houseviewing.fixture.PreAnalysisFixture;
import com.house.houseviewing.fixture.PreReportFixture;
import com.house.houseviewing.fixture.UserFixture;
import com.house.houseviewing.domain.user.entity.UserEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class PreAnalysisEntityTest {

    @Test
    @DisplayName("분석 추가")
    void 분석_추가(){
        UserEntity user = UserFixture.createDefaultWithId(1L);
        PreAnalysisEntity analysis = PreAnalysisFixture.createWithId(user, 1L);
        analysis.addUser(user);

        assertThat(analysis.getUser()).isEqualTo(user);
    }

    @Test
    @DisplayName("리포트 추가")
    void 리포트_추가(){
        UserEntity user = UserFixture.createDefaultWithId(1L);
        PreAnalysisEntity analysis = PreAnalysisFixture.createWithId(user, 1L);
        PreReportEntity report = PreReportFixture.createDefault().build();

        analysis.addReport(report);

        assertThat(analysis.getPreReportEntity()).isEqualTo(report);
    }

    @Test
    @DisplayName("빌더 패턴 테스트")
    void 빌더_패턴(){
        PreAnalysisEntity analysis = PreAnalysisEntity.builder()
                .nickname("테스트")
                .rawData("{\"data\": true}")
                .mainReason("테스트 이유")
                .address(AddressFixture.createAddress().build())
                .riskLevel(RiskLevel.SAFE)
                .ltvScore(100)
                .build();

        assertThat(analysis.getNickname()).isEqualTo("테스트");
        assertThat(analysis.getRawData()).isEqualTo("{\"data\": true}");
        assertThat(analysis.getMainReason()).isEqualTo("테스트 이유");
        assertThat(analysis.getRiskLevel()).isEqualTo(RiskLevel.SAFE);
        assertThat(analysis.getLtvScore()).isEqualTo(100);
    }
}
