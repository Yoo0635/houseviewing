package com.house.houseviewing.domain.auth;

import com.house.houseviewing.domain.auth.model.CustomUserDetails;
import com.house.houseviewing.domain.auth.service.CustomUserDetailsService;
import com.house.houseviewing.domain.user.entity.UserEntity;
import com.house.houseviewing.domain.user.repository.UserRepository;
import com.house.houseviewing.fixture.UserFixture;
import com.house.houseviewing.global.exception.AppException;
import com.house.houseviewing.global.exception.ExceptionCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CustomUserDetailsServiceTest {

    @InjectMocks CustomUserDetailsService customUserDetailsService;

    @Mock UserRepository userRepository;

    @Nested
    @DisplayName("사용자 정보 로드")
    class LoadUserByUsername {

        @Test
        @DisplayName("성공")
        void 성공(){
            UserEntity user = UserFixture.createDefaultWithId(1L);
            given(userRepository.findByLoginId("yooyoo9191")).willReturn(Optional.of(user));

            UserDetails result = customUserDetailsService.loadUserByUsername("yooyoo9191");

            assertThat(result).isNotNull();
            assertThat(result.getUsername()).isEqualTo("yooyoo9191");
        }

        @Test
        @DisplayName("실패: 사용자를 찾을 수 없음")
        void 사용자_없음(){
            given(userRepository.findByLoginId("unknown")).willReturn(Optional.empty());

            assertThatThrownBy(() -> customUserDetailsService.loadUserByUsername("unknown"))
                    .isInstanceOf(AppException.class)
                    .extracting("exceptionCode")
                    .isEqualTo(ExceptionCode.USER_NOT_FOUND);
        }
    }
}
