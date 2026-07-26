package com.house.houseviewing.domain.analysis.postanalysis;

import com.house.houseviewing.domain.analysis.postanalysis.dto.response.AnalysisResponse;
import com.house.houseviewing.domain.analysis.postanalysis.entity.PostAnalysisEntity;
import com.house.houseviewing.domain.analysis.postanalysis.enums.AnalysisType;
import com.house.houseviewing.domain.analysis.postanalysis.repository.PostAnalysisRepository;
import com.house.houseviewing.domain.analysis.postanalysis.service.PostAnalysisService;
import com.house.houseviewing.domain.contract.entity.ContractEntity;
import com.house.houseviewing.domain.contract.repository.ContractRepository;
import com.house.houseviewing.domain.house.entity.HouseEntity;
import com.house.houseviewing.domain.house.repository.HouseRepository;
import com.house.houseviewing.domain.report.postreport.entity.PostReportEntity;
import com.house.houseviewing.domain.user.entity.UserEntity;
import com.house.houseviewing.fixture.ContractFixture;
import com.house.houseviewing.fixture.HouseFixture;
import com.house.houseviewing.fixture.PostAnalysisFixture;
import com.house.houseviewing.fixture.PostReportFixture;
import com.house.houseviewing.fixture.UserFixture;
import com.house.houseviewing.global.exception.AppException;
import com.house.houseviewing.global.exception.ExceptionCode;
import com.house.houseviewing.global.file.diff.service.SnapshotDiffAnalysisService;
import com.house.houseviewing.global.file.snapshot.service.SnapshotAnalysisService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PostAnalysisServiceTest {

    @InjectMocks PostAnalysisService postAnalysisService;

    @Mock ContractRepository contractRepository;
    @Mock PostAnalysisRepository postAnalysisRepository;
    @Mock SnapshotAnalysisService snapshotAnalysisService;
    @Mock SnapshotDiffAnalysisService snapshotDiffAnalysisService;
    @Mock HouseRepository houseRepository;

    @Nested
    @DisplayName("사후 분석 등록")
    class PostRegister {

        @Test
        @DisplayName("성공")
        void 성공(){
            UserEntity user = UserFixture.createDefaultWithId(1L);
            HouseEntity house = HouseFixture.createWithUserAndId(user, 1L);
            ContractEntity contract = ContractFixture.createWithHouseAndId(house, 1L);
            PostAnalysisEntity analysis = PostAnalysisFixture.createWithId(house, 1L);
            MultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", "data".getBytes());

            given(houseRepository.findById(anyLong())).willReturn(Optional.of(house));
            given(contractRepository.findTopByHouseIdOrderByCreatedAtDesc(anyLong())).willReturn(Optional.of(contract));
            given(snapshotAnalysisService.postAnalyze(any(MultipartFile.class))).willReturn(analysis);
            given(postAnalysisRepository.save(any(PostAnalysisEntity.class))).willReturn(analysis);

            PostAnalysisEntity result = postAnalysisService.postRegister(1L, file);

            assertThat(result).isNotNull();
            assertThat(result.getRiskLevel()).isNotNull();
            verify(postAnalysisRepository).save(any(PostAnalysisEntity.class));
        }

        @Test
        @DisplayName("실패: 집을 찾을 수 없음")
        void 집_없음(){
            MultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", "data".getBytes());
            given(houseRepository.findById(anyLong())).willReturn(Optional.empty());

            assertThatThrownBy(() -> postAnalysisService.postRegister(1L, file))
                    .isInstanceOf(AppException.class)
                    .extracting("exceptionCode")
                    .isEqualTo(ExceptionCode.HOUSE_NOT_FOUND);
        }

        @Test
        @DisplayName("실패: 계약을 찾을 수 없음")
        void 계약_없음(){
            UserEntity user = UserFixture.createDefaultWithId(1L);
            HouseEntity house = HouseFixture.createWithUserAndId(user, 1L);
            MultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", "data".getBytes());

            given(houseRepository.findById(anyLong())).willReturn(Optional.of(house));
            given(contractRepository.findTopByHouseIdOrderByCreatedAtDesc(anyLong())).willReturn(Optional.empty());

            assertThatThrownBy(() -> postAnalysisService.postRegister(1L, file))
                    .isInstanceOf(AppException.class)
                    .extracting("exceptionCode")
                    .isEqualTo(ExceptionCode.CONTRACT_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("사후 분석 목록 조회")
    class GetPostAnalyses {

        @Test
        @DisplayName("성공")
        void 성공(){
            UserEntity user = UserFixture.createDefaultWithId(1L);
            HouseEntity house = HouseFixture.createWithUserAndId(user, 1L);
            PostAnalysisEntity analysis = PostAnalysisFixture.createWithId(house, 1L);
            PostReportEntity report = PostReportFixture.createDefault()
                    .id(101L)
                    .build();
            report.addRegistryAnalysis(analysis);
            List<PostAnalysisEntity> analyses = List.of(analysis);

            given(postAnalysisRepository.findAllByUserId(anyLong())).willReturn(analyses);

            List<AnalysisResponse> result = postAnalysisService.getPostAnalyses(1L);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getPdfReportId()).isEqualTo(101L);
        }
    }

    @Nested
    @DisplayName("차이 분석 목록 조회")
    class GetDiffAnalyses {

        @Test
        @DisplayName("성공")
        void 성공(){
            UserEntity user = UserFixture.createDefaultWithId(1L);
            HouseEntity house = HouseFixture.createWithUserAndId(user, 1L);
            PostAnalysisEntity analysis = PostAnalysisFixture.createDiffWithId(house, 1L);
            PostReportEntity report = PostReportFixture.createDefault()
                    .id(102L)
                    .build();
            report.addRegistryAnalysis(analysis);
            List<PostAnalysisEntity> analyses = List.of(analysis);

            given(postAnalysisRepository.findAllByUserIdAndAnalysisType(anyLong(), eq(AnalysisType.DIFF)))
                    .willReturn(analyses);

            List<AnalysisResponse> result = postAnalysisService.getDiffAnalyses(1L);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getPdfReportId()).isEqualTo(102L);
        }
    }

    @Nested
    @DisplayName("차이 분석 등록")
    class DiffRegister {

        @Test
        @DisplayName("성공")
        void 성공(){
            UserEntity user = UserFixture.createDefaultWithId(1L);
            HouseEntity house = HouseFixture.createWithUserAndId(user, 1L);
            ContractEntity contract = ContractFixture.createWithHouseAndId(house, 1L);
            PostAnalysisEntity analysis = PostAnalysisFixture.createDiffWithId(house, 1L);
            String snapshot = "{\"new\": \"data\"}";

            given(houseRepository.findById(anyLong())).willReturn(Optional.of(house));
            given(contractRepository.findTopByHouseIdOrderByCreatedAtDesc(anyLong())).willReturn(Optional.of(contract));
            given(snapshotDiffAnalysisService.diffAnalyze(anyString())).willReturn(analysis);
            given(postAnalysisRepository.save(any(PostAnalysisEntity.class))).willReturn(analysis);

            PostAnalysisEntity result = postAnalysisService.diffRegister(1L, snapshot);

            assertThat(result).isNotNull();
            assertThat(result.getAnalysisType()).isEqualTo(AnalysisType.DIFF);
            verify(postAnalysisRepository).save(any(PostAnalysisEntity.class));
        }
    }
}
