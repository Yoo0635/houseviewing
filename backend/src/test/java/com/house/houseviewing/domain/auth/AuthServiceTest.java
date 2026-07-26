package com.house.houseviewing.domain.auth;

import com.house.houseviewing.domain.auth.dto.request.LoginRequest;
import com.house.houseviewing.domain.auth.dto.request.ReissueRequest;
import com.house.houseviewing.domain.auth.dto.response.LoginResponse;
import com.house.houseviewing.domain.auth.dto.response.ReissueResponse;
import com.house.houseviewing.domain.auth.jwt.JwtTokenProvider;
import com.house.houseviewing.domain.auth.model.CustomUserDetails;
import com.house.houseviewing.domain.auth.service.AuthService;
import com.house.houseviewing.domain.auth.service.RefreshTokenService;
import com.house.houseviewing.domain.auth.service.TokenBlacklistService;
import com.house.houseviewing.domain.user.entity.UserEntity;
import com.house.houseviewing.fixture.AuthFixture;
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
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @InjectMocks AuthService authService;

    @Mock JwtTokenProvider jwtTokenProvider;
    @Mock RefreshTokenService refreshTokenService;
    @Mock AuthenticationManager authenticationManager;
    @Mock TokenBlacklistService tokenBlacklistService;

    @Nested
    @DisplayName("로그인")
    class Login {

        @Test
        @DisplayName("성공")
        void 성공(){
            UserEntity user = UserFixture.createDefaultWithId(1L);
            CustomUserDetails userDetails = new CustomUserDetails(user);
            LoginRequest request = AuthFixture.createLoginRequest().build();
            Authentication authentication = mock(Authentication.class);

            given(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .willReturn(authentication);
            given(authentication.getPrincipal()).willReturn(userDetails);
            given(jwtTokenProvider.createAccessToken(anyLong(), anyString())).willReturn("access-token");
            given(jwtTokenProvider.createRefreshToken(anyLong(), anyString())).willReturn("refresh-token");
            given(jwtTokenProvider.getRefreshTokenExpiration()).willReturn(604800000L);

            LoginResponse result = authService.login(request);

            assertThat(result).isNotNull();
            assertThat(result.getAccessToken()).isEqualTo("access-token");
            assertThat(result.getRefreshToken()).isEqualTo("refresh-token");
            assertThat(result.getUserId()).isEqualTo(1L);
            verify(refreshTokenService).saveRefreshToken(anyLong(), eq("refresh-token"), anyLong());
        }
    }

    @Nested
    @DisplayName("토큰 갱신")
    class Reissue {

        @Test
        @DisplayName("성공")
        void 성공(){
            ReissueRequest request = AuthFixture.createReissueRequest("refresh-token").build();

            given(jwtTokenProvider.validateToken(anyString())).willReturn(true);
            given(jwtTokenProvider.getUserId(anyString())).willReturn(1L);
            given(jwtTokenProvider.getLoginId(anyString())).willReturn("yooyoo9191");
            given(refreshTokenService.getRefreshToken(anyLong())).willReturn("refresh-token");
            given(jwtTokenProvider.createAccessToken(anyLong(), anyString())).willReturn("new-access-token");

            ReissueResponse result = authService.reissue(request);

            assertThat(result).isNotNull();
            assertThat(result.getAccessToken()).isEqualTo("new-access-token");
        }

        @Test
        @DisplayName("실패: 저장된 토큰과 불일치")
        void 토큰_불일치(){
            ReissueRequest request = AuthFixture.createReissueRequest("wrong-token").build();

            given(jwtTokenProvider.validateToken(anyString())).willReturn(true);
            given(jwtTokenProvider.getUserId(anyString())).willReturn(1L);
            given(jwtTokenProvider.getLoginId(anyString())).willReturn("yooyoo9191");
            given(refreshTokenService.getRefreshToken(anyLong())).willReturn("different-token");

            assertThatThrownBy(() -> authService.reissue(request))
                    .isInstanceOf(AppException.class)
                    .extracting("exceptionCode")
                    .isEqualTo(ExceptionCode.INVALID_TOKEN);
        }

        @Test
        @DisplayName("실패: 저장된 토큰이 없음")
        void 토큰_없음(){
            ReissueRequest request = AuthFixture.createReissueRequest("refresh-token").build();

            given(jwtTokenProvider.validateToken(anyString())).willReturn(true);
            given(jwtTokenProvider.getUserId(anyString())).willReturn(1L);
            given(jwtTokenProvider.getLoginId(anyString())).willReturn("yooyoo9191");
            given(refreshTokenService.getRefreshToken(anyLong())).willReturn(null);

            assertThatThrownBy(() -> authService.reissue(request))
                    .isInstanceOf(AppException.class)
                    .extracting("exceptionCode")
                    .isEqualTo(ExceptionCode.INVALID_TOKEN);
        }
    }

    @Nested
    @DisplayName("로그아웃")
    class Logout {

        @Test
        @DisplayName("성공")
        void 성공(){
            String header = "Bearer access-token";

            given(jwtTokenProvider.getRemainingTime(anyString())).willReturn(3600000L);
            given(jwtTokenProvider.getUserId(anyString())).willReturn(1L);

            authService.logout(header);

            verify(tokenBlacklistService).blacklistToken(anyString(), anyLong());
            verify(refreshTokenService).deleteRefreshToken(anyLong());
        }

        @Test
        @DisplayName("실패: 잘못된 헤더")
        void 잘못된_헤더(){
            String header = "InvalidHeader";

            assertThatThrownBy(() -> authService.logout(header))
                    .isInstanceOf(AppException.class)
                    .extracting("exceptionCode")
                    .isEqualTo(ExceptionCode.INVALID_HEADER);
        }

        @Test
        @DisplayName("실패: null 헤더")
        void null_헤더(){
            assertThatThrownBy(() -> authService.logout(null))
                    .isInstanceOf(AppException.class)
                    .extracting("exceptionCode")
                    .isEqualTo(ExceptionCode.INVALID_HEADER);
        }

        @Test
        @DisplayName("성공: Bearer 토큰")
        void 성공_베어러(){
            String header = "Bearer access-token";

            given(jwtTokenProvider.getRemainingTime(anyString())).willReturn(3600000L);
            given(jwtTokenProvider.getUserId(anyString())).willReturn(1L);

            authService.logout(header);

            verify(tokenBlacklistService).blacklistToken(anyString(), anyLong());
            verify(refreshTokenService).deleteRefreshToken(anyLong());
        }
    }
}
