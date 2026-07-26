package com.house.houseviewing.domain.report.postreport;

import com.house.houseviewing.domain.analysis.postanalysis.entity.PostAnalysisEntity;
import com.house.houseviewing.domain.analysis.postanalysis.enums.AnalysisType;
import com.house.houseviewing.domain.analysis.postanalysis.repository.PostAnalysisRepository;
import com.house.houseviewing.domain.common.RiskLevel;
import com.house.houseviewing.domain.contract.entity.ContractEntity;
import com.house.houseviewing.domain.contract.dto.request.ContractRegisterRequest;
import com.house.houseviewing.domain.contract.service.ContractService;
import com.house.houseviewing.domain.contract.repository.ContractRepository;
import com.house.houseviewing.domain.house.entity.HouseEntity;
import com.house.houseviewing.domain.house.dto.request.HouseRegisterRequest;
import com.house.houseviewing.domain.house.service.HouseService;
import com.house.houseviewing.domain.house.repository.HouseRepository;
import com.house.houseviewing.domain.report.postreport.entity.PostReportEntity;
import com.house.houseviewing.domain.report.postreport.repository.PostReportRepository;
import com.house.houseviewing.domain.report.postreport.service.PostReportService;
import com.house.houseviewing.domain.user.entity.UserEntity;
import com.house.houseviewing.domain.user.dto.request.UserRegisterRequest;
import com.house.houseviewing.domain.user.repository.UserRepository;
import com.house.houseviewing.domain.user.service.UserService;
import com.house.houseviewing.fixture.AddressFixture;
import com.house.houseviewing.fixture.ContractFixture;
import com.house.houseviewing.fixture.HouseFixture;
import com.house.houseviewing.fixture.UserFixture;
import com.house.houseviewing.global.external.kakao.service.KakaoAddress;
import com.house.houseviewing.global.file.diff.service.SnapshotDiffAnalysisService;
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
public class PostReportIntegrationTest {

    @Autowired PostReportService postReportService;
    @Autowired PostReportRepository postReportRepository;
    @Autowired PostAnalysisRepository postAnalysisRepository;
    @Autowired HouseService houseService;
    @Autowired UserService userService;
    @Autowired ContractService contractService;
    @Autowired UserRepository userRepository;
    @Autowired HouseRepository houseRepository;
    @Autowired ContractRepository contractRepository;

    @MockitoBean KakaoAddress kakaoAddress;
    @MockitoBean PythonEngineClient pythonEngineClient;
    @MockitoBean SnapshotAnalysisService snapshotAnalysisService;
    @MockitoBean SnapshotDiffAnalysisService snapshotDiffAnalysisService;
    @MockitoBean PdfReportTransferAndReceiveService pdfReportTransferAndReceiveService;

    @Test
    @DisplayName("사후 리포트 저장 및 조회")
    void 사후_리포트_저장(){
        UserEntity user = getUserEntity();
        HouseEntity house = getHouseEntity(user);
        ContractEntity contract = getContract(house);

        PostAnalysisEntity analysis = PostAnalysisEntity.builder()
                .riskLevel(RiskLevel.SAFE)
                .analysisType(AnalysisType.BASIC)
                .mainReason("안전")
                .ltvScore(85)
                .rawData("{\"test\": true}")
                .build();
        analysis.addHouse(house);
        analysis.addContract(contract);
        PostAnalysisEntity savedAnalysis = postAnalysisRepository.save(analysis);

        given(pdfReportTransferAndReceiveService.postTransferAndReceive(any()))
                .willReturn(PdfUploadResult.builder()
                        .pdfKey("post-key")
                        .pdfPath("/post/path")
                        .pdfName("post.pdf")
                        .pdfSizeBytes(2048L)
                        .build());

        PostReportEntity report = postReportService.postRegister(savedAnalysis);

        assertThat(report).isNotNull();
        assertThat(report.getPdfKey()).isEqualTo("post-key");
        assertThat(report.getPdfPath()).isEqualTo("/post/path");
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

    private ContractEntity getContract(HouseEntity house) {
        ContractEntity contract = ContractFixture.createDefault(house).build();
        ContractRegisterRequest request = ContractFixture.createRegister(contract)
                .houseId(house.getId()).build();
        contractService.register(request);
        return contractRepository.findTopByHouseIdOrderByCreatedAtDesc(house.getId()).orElseThrow();
    }
}
