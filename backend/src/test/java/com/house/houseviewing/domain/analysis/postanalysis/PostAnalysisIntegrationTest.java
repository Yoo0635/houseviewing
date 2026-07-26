package com.house.houseviewing.domain.analysis.postanalysis;

import com.house.houseviewing.domain.analysis.postanalysis.entity.PostAnalysisEntity;
import com.house.houseviewing.domain.analysis.postanalysis.enums.AnalysisType;
import com.house.houseviewing.domain.analysis.postanalysis.repository.PostAnalysisRepository;
import com.house.houseviewing.domain.analysis.postanalysis.service.PostAnalysisService;
import com.house.houseviewing.domain.common.RiskLevel;
import com.house.houseviewing.domain.contract.entity.ContractEntity;
import com.house.houseviewing.domain.contract.dto.request.ContractRegisterRequest;
import com.house.houseviewing.domain.contract.repository.ContractRepository;
import com.house.houseviewing.domain.contract.service.ContractService;
import com.house.houseviewing.domain.house.entity.HouseEntity;
import com.house.houseviewing.domain.house.dto.request.HouseRegisterRequest;
import com.house.houseviewing.domain.house.repository.HouseRepository;
import com.house.houseviewing.domain.house.service.HouseService;
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
import com.house.houseviewing.global.file.snapshot.service.SnapshotAnalysisService;
import com.house.houseviewing.infrastructure.python.PythonEngineClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.*;

@SpringBootTest
@Transactional
public class PostAnalysisIntegrationTest {

    @Autowired PostAnalysisService postAnalysisService;
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

    @Test
    @DisplayName("사후 분석 목록 조회")
    void 사후_분석_목록_조회(){
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
        postAnalysisRepository.save(analysis);

        List<PostAnalysisEntity> analyses = postAnalysisRepository.findAllByUserId(user.getId());

        assertThat(analyses).hasSize(1);
    }

    @Test
    @DisplayName("차이 분석 목록 조회")
    void 차이_분석_목록_조회(){
        UserEntity user = getUserEntity();
        HouseEntity house = getHouseEntity(user);
        ContractEntity contract = getContract(house);

        PostAnalysisEntity analysis = PostAnalysisEntity.builder()
                .riskLevel(RiskLevel.WARNING)
                .analysisType(AnalysisType.DIFF)
                .mainReason("주의")
                .ltvScore(70)
                .rawData("{\"diff\": true}")
                .build();
        analysis.addHouse(house);
        analysis.addContract(contract);
        postAnalysisRepository.save(analysis);

        List<PostAnalysisEntity> diffAnalyses = postAnalysisRepository
                .findAllByUserIdAndAnalysisType(user.getId(), AnalysisType.DIFF);

        assertThat(diffAnalyses).hasSize(1);
        assertThat(diffAnalyses.get(0).getAnalysisType()).isEqualTo(AnalysisType.DIFF);
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
