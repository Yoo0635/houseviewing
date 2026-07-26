package com.house.houseviewing.domain.analysis.postanalysis;

import com.house.houseviewing.domain.analysis.postanalysis.entity.PostAnalysisEntity;
import com.house.houseviewing.domain.analysis.postanalysis.enums.AnalysisType;
import com.house.houseviewing.domain.common.RiskLevel;
import com.house.houseviewing.domain.contract.entity.ContractEntity;
import com.house.houseviewing.domain.house.entity.HouseEntity;
import com.house.houseviewing.domain.report.postreport.entity.PostReportEntity;
import com.house.houseviewing.domain.user.entity.UserEntity;
import com.house.houseviewing.fixture.AddressFixture;
import com.house.houseviewing.fixture.ContractFixture;
import com.house.houseviewing.fixture.HouseFixture;
import com.house.houseviewing.fixture.PostAnalysisFixture;
import com.house.houseviewing.fixture.PostReportFixture;
import com.house.houseviewing.fixture.UserFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class PostAnalysisEntityTest {

    @Test
    @DisplayName("집 추가")
    void 집_추가(){
        UserEntity user = UserFixture.createDefaultWithId(1L);
        HouseEntity house = HouseFixture.createWithUserAndId(user, 1L);
        PostAnalysisEntity analysis = PostAnalysisFixture.createWithId(house, 1L);

        analysis.addHouse(house);

        assertThat(analysis.getHouse()).isEqualTo(house);
    }

    @Test
    @DisplayName("계약 추가")
    void 계약_추가(){
        UserEntity user = UserFixture.createDefaultWithId(1L);
        HouseEntity house = HouseFixture.createWithUserAndId(user, 1L);
        ContractEntity contract = ContractFixture.createWithHouseAndId(house, 1L);
        PostAnalysisEntity analysis = PostAnalysisFixture.createWithId(house, 1L);

        analysis.addContract(contract);

        assertThat(analysis.getContract()).isEqualTo(contract);
    }

    @Test
    @DisplayName("PDF 리포트 추가")
    void PDF_리포트_추가(){
        UserEntity user = UserFixture.createDefaultWithId(1L);
        HouseEntity house = HouseFixture.createWithUserAndId(user, 1L);
        PostAnalysisEntity analysis = PostAnalysisFixture.createWithId(house, 1L);
        PostReportEntity report = PostReportFixture.createDefault().build();

        analysis.addPdfReport(report);

        assertThat(analysis.getPdfReport()).isEqualTo(report);
    }

    @Test
    @DisplayName("빌더 패턴 테스트")
    void 빌더_패턴(){
        PostAnalysisEntity analysis = PostAnalysisEntity.builder()
                .riskLevel(RiskLevel.WARNING)
                .analysisType(AnalysisType.DIFF)
                .mainReason("주의 필요")
                .ltvScore(65)
                .rawData("{\"warning\": true}")
                .build();

        assertThat(analysis.getRiskLevel()).isEqualTo(RiskLevel.WARNING);
        assertThat(analysis.getAnalysisType()).isEqualTo(AnalysisType.DIFF);
        assertThat(analysis.getMainReason()).isEqualTo("주의 필요");
        assertThat(analysis.getLtvScore()).isEqualTo(65);
        assertThat(analysis.getRawData()).isEqualTo("{\"warning\": true}");
    }
}
