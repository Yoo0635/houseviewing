package com.house.houseviewing.domain.report.prereport;

import com.house.houseviewing.domain.analysis.preanalysis.entity.PreAnalysisEntity;
import com.house.houseviewing.domain.analysis.preanalysis.repository.PreAnalysisRepository;
import com.house.houseviewing.domain.analysis.preanalysis.service.PreAnalysisService;
import com.house.houseviewing.domain.analysis.preanalysis.dto.request.PreContractDiagnosisRequest;
import com.house.houseviewing.domain.house.entity.HouseEntity;
import com.house.houseviewing.domain.house.dto.request.HouseRegisterRequest;
import com.house.houseviewing.domain.house.service.HouseService;
import com.house.houseviewing.domain.house.repository.HouseRepository;
import com.house.houseviewing.domain.report.prereport.entity.PreReportEntity;
import com.house.houseviewing.domain.report.prereport.repository.PreReportRepository;
import com.house.houseviewing.domain.report.prereport.service.PreReportService;
import com.house.houseviewing.domain.user.entity.UserEntity;
import com.house.houseviewing.domain.user.dto.request.UserRegisterRequest;
import com.house.houseviewing.domain.user.repository.UserRepository;
import com.house.houseviewing.domain.user.service.UserService;
import com.house.houseviewing.domain.common.RiskLevel;
import com.house.houseviewing.fixture.AddressFixture;
import com.house.houseviewing.fixture.HouseFixture;
import com.house.houseviewing.fixture.UserFixture;
import com.house.houseviewing.global.external.kakao.service.KakaoAddress;
import com.house.houseviewing.global.file.pdf.dto.PdfUploadResult;
import com.house.houseviewing.global.file.pdf.service.PdfReportTransferAndReceiveService;
import com.house.houseviewing.global.file.snapshot.service.SnapshotAnalysisService;
import com.house.houseviewing.infrastructure.python.PythonEngineClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

@SpringBootTest
@Transactional
public class PreReportIntegrationTest {

    @Autowired PreReportService preReportService;
    @Autowired PreReportRepository preReportRepository;
    @Autowired PreAnalysisService preAnalysisService;
    @Autowired PreAnalysisRepository preAnalysisRepository;
    @Autowired HouseService houseService;
    @Autowired UserService userService;
    @Autowired UserRepository userRepository;
    @Autowired HouseRepository houseRepository;

    @MockitoBean KakaoAddress kakaoAddress;
    @MockitoBean PythonEngineClient pythonEngineClient;
    @MockitoBean SnapshotAnalysisService snapshotAnalysisService;
    @MockitoBean PdfReportTransferAndReceiveService pdfReportTransferAndReceiveService;

    @Test
    @DisplayName("사전 리포트 등록")
    void 사전_리포트_등록(){
        given(snapshotAnalysisService.preAnalyze(anyString(), any(), any()))
                .willAnswer(invocation -> PreAnalysisEntity.builder()
                        .nickname(invocation.getArgument(0))
                        .rawData("{\"test\": true}")
                        .mainReason("안전")
                        .address(AddressFixture.createAddress().build())
                        .ltvScore(80)
                        .riskLevel(RiskLevel.SAFE)
                        .build());

        given(pdfReportTransferAndReceiveService.preTransferAndReceive(any()))
                .willReturn(PdfUploadResult.builder()
                        .pdfKey("test-key")
                        .pdfPath("/test/path")
                        .pdfName("test.pdf")
                        .pdfSizeBytes(1024L)
                        .build());

        UserEntity user = getUserEntity();
        HouseEntity house = getHouseEntity(user);

        PreContractDiagnosisRequest request = PreContractDiagnosisRequest.builder()
                .nickname("테스트분석")
                .address("서울 강남구")
                .build();

        PreAnalysisEntity analysis = preAnalysisService.preRegister(user.getId(), request, null);
        PreReportEntity report = preReportService.preRegister(analysis);

        assertThat(report).isNotNull();
        assertThat(report.getPdfKey()).isEqualTo("test-key");
        assertThat(report.getPdfPath()).isEqualTo("/test/path");
        assertThat(report.getAnalysis().getId()).isEqualTo(analysis.getId());
    }

    private UserEntity getUserEntity() {
        UserEntity user = UserFixture.createDefault().build();
        UserRegisterRequest requestUser = UserFixture.createRegister(user).build();
        userService.register(requestUser);
        return userRepository.findByLoginId(user.getLoginId()).orElseThrow();
    }

    private HouseEntity getHouseEntity(UserEntity user) {
        given(kakaoAddress.parsingAddress(anyString())).willReturn(AddressFixture.createAddress().build());
        HouseEntity house = HouseFixture.createDefault(user).build();
        HouseRegisterRequest request = HouseFixture.createRegister(house).build();
        houseService.register(user.getId(), request);
        return houseRepository.findByUserId(user.getId()).stream().findFirst().orElseThrow();
    }
}
