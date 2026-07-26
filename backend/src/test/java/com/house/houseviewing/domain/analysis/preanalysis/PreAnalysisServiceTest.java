package com.house.houseviewing.domain.analysis.preanalysis;

import com.house.houseviewing.domain.analysis.postanalysis.dto.response.AnalysisResponse;
import com.house.houseviewing.domain.analysis.preanalysis.dto.request.PreContractDiagnosisRequest;
import com.house.houseviewing.domain.analysis.preanalysis.entity.PreAnalysisEntity;
import com.house.houseviewing.domain.analysis.preanalysis.repository.PreAnalysisRepository;
import com.house.houseviewing.domain.report.prereport.entity.PreReportEntity;
import com.house.houseviewing.domain.analysis.preanalysis.service.PreAnalysisService;
import com.house.houseviewing.domain.common.Address;
import com.house.houseviewing.domain.user.entity.UserEntity;
import com.house.houseviewing.domain.user.repository.UserRepository;
import com.house.houseviewing.fixture.AddressFixture;
import com.house.houseviewing.fixture.PreAnalysisFixture;
import com.house.houseviewing.fixture.PreReportFixture;
import com.house.houseviewing.fixture.UserFixture;
import com.house.houseviewing.global.exception.AppException;
import com.house.houseviewing.global.exception.ExceptionCode;
import com.house.houseviewing.global.external.kakao.service.KakaoAddress;
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

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PreAnalysisServiceTest {

    @InjectMocks PreAnalysisService preAnalysisService;

    @Mock UserRepository userRepository;
    @Mock SnapshotAnalysisService snapshotAnalysisService;
    @Mock PreAnalysisRepository preAnalysisRepository;
    @Mock KakaoAddress kakaoAddress;

    @Nested
    @DisplayName("사전 분석 등록")
    class PreRegister {

        @Test
        @DisplayName("성공")
        void 성공(){
            UserEntity user = UserFixture.createDefaultWithId(1L);
            PreAnalysisEntity analysis = PreAnalysisFixture.createWithId(user, 1L);
            PreContractDiagnosisRequest request = PreAnalysisFixture.createRequest().build();
            MultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", "data".getBytes());
            Address address = AddressFixture.createAddress().build();

            given(userRepository.findById(anyLong())).willReturn(Optional.of(user));
            given(kakaoAddress.parsingAddress(anyString())).willReturn(address);
            given(snapshotAnalysisService.preAnalyze(anyString(), any(Address.class), any(MultipartFile.class)))
                    .willReturn(analysis);
            given(preAnalysisRepository.save(any(PreAnalysisEntity.class))).willAnswer(inv -> analysis);
            given(preAnalysisRepository.existsByUserId(anyLong())).willReturn(false);

            PreAnalysisEntity result = preAnalysisService.preRegister(1L, request, file);

            assertThat(result).isNotNull();
            verify(preAnalysisRepository).save(any(PreAnalysisEntity.class));
        }

        @Test
        @DisplayName("실패: 유저를 찾을 수 없음")
        void 유저_없음(){
            PreContractDiagnosisRequest request = PreAnalysisFixture.createRequest().build();
            MultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", "data".getBytes());

            given(userRepository.findById(anyLong())).willReturn(Optional.empty());

            assertThatThrownBy(() -> preAnalysisService.preRegister(1L, request, file))
                    .isInstanceOf(AppException.class)
                    .extracting("exceptionCode")
                    .isEqualTo(ExceptionCode.USER_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("사전 분석 목록 조회")
    class GetPreAnalyses {

        @Test
        @DisplayName("성공")
        void 성공(){
            UserEntity user = UserFixture.createDefaultWithId(1L);
            PreAnalysisEntity analysis = PreAnalysisFixture.createWithId(user, 1L);
            PreReportEntity report = PreReportFixture.createDefault()
                    .id(201L)
                    .build();
            report.addAnalysis(analysis);
            List<PreAnalysisEntity> analyses = List.of(analysis);

            given(preAnalysisRepository.findAllByUserId(anyLong())).willReturn(analyses);

            List<AnalysisResponse> result = preAnalysisService.getPreAnalyses(1L);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getPdfReportId()).isEqualTo(201L);
            assertThat(result.get(0).getNickname()).isEqualTo("테스트분석");
        }

        @Test
        @DisplayName("빈 목록")
        void 빈_목록(){
            given(preAnalysisRepository.findAllByUserId(anyLong())).willReturn(List.of());

            List<AnalysisResponse> result = preAnalysisService.getPreAnalyses(1L);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("무료 진단 가능 여부 검증")
    class ValidateFreeDiagnosis {

        @Test
        @DisplayName("프리미엄 유저는 무료 진단 가능")
        void 프리미엄_유저() throws Exception{
            UserEntity user = UserFixture.createPremium();
            Field idField = UserEntity.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(user, 1L);

            preAnalysisService.validateFreeDiagnosisAvailable(user);
        }

        @Test
        @DisplayName("무료 유저이고 첫 진단이면 가능")
        void 무료_첫_진단(){
            UserEntity user = UserFixture.createDefaultWithId(1L);
            given(preAnalysisRepository.existsByUserId(anyLong())).willReturn(false);

            preAnalysisService.validateFreeDiagnosisAvailable(user);
        }

        @Test
        @DisplayName("무료 유저이고 이미 진단했으면 예외")
        void 무료_이미_진단(){
            UserEntity user = UserFixture.createDefaultWithId(1L);
            given(preAnalysisRepository.existsByUserId(anyLong())).willReturn(true);

            assertThatThrownBy(() -> preAnalysisService.validateFreeDiagnosisAvailable(user))
                    .isInstanceOf(AppException.class)
                    .extracting("exceptionCode")
                    .isEqualTo(ExceptionCode.FREE_DIAGNOSIS_ALREADY_USED);
        }
    }
}
