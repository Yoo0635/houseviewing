package com.house.houseviewing.domain.house;

import com.house.houseviewing.domain.house.entity.HouseEntity;
import com.house.houseviewing.domain.house.dto.request.HouseRegisterRequest;
import com.house.houseviewing.domain.house.dto.response.HouseRegisterResponse;
import com.house.houseviewing.domain.house.repository.HouseRepository;
import com.house.houseviewing.domain.house.service.HousePersistenceService;
import com.house.houseviewing.domain.house.service.HouseService;
import com.house.houseviewing.domain.user.entity.UserEntity;
import com.house.houseviewing.fixture.HouseFixture;
import com.house.houseviewing.fixture.UserFixture;
import com.house.houseviewing.global.exception.AppException;
import com.house.houseviewing.global.exception.ExceptionCode;
import com.house.houseviewing.global.external.kakao.service.KakaoAddress;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.inOrder;

@ExtendWith(MockitoExtension.class)
class HouseServiceTest {

    HouseService houseService;

    @Mock HouseRepository houseRepository;

    @Mock KakaoAddress kakaoAddress;
    @Mock HousePersistenceService housePersistenceService;

    @BeforeEach
    void setUp() {
        houseService = new HouseService(
                houseRepository,
                kakaoAddress,
                housePersistenceService,
                null,
                null
        );
    }

    @Nested
    @DisplayName("집 등록")
    class Register{

        @Test
        @DisplayName("성공")
        void 성공(){
            UserEntity user = UserFixture.createPremium();
            HouseEntity house = HouseFixture.createDefault(user).build();
            HouseRegisterRequest request = HouseFixture.createRegister(house).build();
            given(kakaoAddress.parsingAddress(anyString())).willReturn(house.getAddress());
            given(housePersistenceService.register(anyLong(), any(HouseRegisterRequest.class), any()))
                    .willReturn(HouseRegisterResponse.from(1L));

            HouseRegisterResponse result = houseService.register(1L, request);

            assertThat(result).isNotNull();
            var inOrder = inOrder(kakaoAddress, housePersistenceService);
            inOrder.verify(kakaoAddress).parsingAddress(request.getOriginAddress());
            inOrder.verify(housePersistenceService).register(eq(1L), eq(request), eq(house.getAddress()));
        }

        @Test
        @DisplayName("실패: 사용자를 찾을 수 없음")
        void 실패1(){
            UserEntity user = UserFixture.createDefault().build();
            HouseEntity house = HouseFixture.createDefault(user).build();
            HouseRegisterRequest request = HouseFixture.createRegister(house).build();
            given(kakaoAddress.parsingAddress(anyString())).willReturn(house.getAddress());
            given(housePersistenceService.register(anyLong(), any(HouseRegisterRequest.class), any()))
                    .willThrow(new AppException(ExceptionCode.USER_NOT_FOUND));

            assertThatThrownBy(() -> houseService.register(1L, request))
                    .isInstanceOf(AppException.class)
                    .extracting("exceptionCode")
                    .isEqualTo(ExceptionCode.USER_NOT_FOUND);
        }
    }

    @Test
    @DisplayName("집 등록과 수정은 외부 주소 호출을 위해 서비스 트랜잭션을 열지 않음")
    void register_and_edit_do_not_start_service_transaction() throws Exception {
        Method register = HouseService.class.getMethod("register", Long.class, HouseRegisterRequest.class);
        Method edit = HouseService.class.getMethod(
                "editHouse",
                Long.class,
                Long.class,
                com.house.houseviewing.domain.house.dto.request.HouseEditRequest.class
        );

        assertThat(register.getAnnotation(Transactional.class).propagation()).isEqualTo(Propagation.NOT_SUPPORTED);
        assertThat(edit.getAnnotation(Transactional.class).propagation()).isEqualTo(Propagation.NOT_SUPPORTED);
    }

    @Nested
    @DisplayName("집 삭제")
    class Delete{

        @Test
        @DisplayName("성공")
        void 성공(){
            UserEntity user = UserFixture.createDefault().build();
            HouseEntity house = HouseFixture.createDefault(user).build();
            given(houseRepository.findById(anyLong()))
                    .willReturn(Optional.of(house));

            houseService.delete(1L, 1L);

            then(houseRepository).should(times(1)).findById(1L);
        }

        @Test
        @DisplayName("실패: 집을 찾을 수 없음")
        void 실패(){
            given(houseRepository.findById(anyLong()))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> houseService.delete(1L, 1L))
                    .isInstanceOf(AppException.class)
                    .extracting("exceptionCode")
                    .isEqualTo(ExceptionCode.HOUSE_NOT_FOUND);
        }
    }
}
