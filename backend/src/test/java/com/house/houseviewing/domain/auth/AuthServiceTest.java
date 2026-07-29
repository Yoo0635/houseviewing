package com.house.houseviewing.domain.auth;

import com.house.houseviewing.domain.auth.dto.request.LoginRequest;
import com.house.houseviewing.domain.auth.dto.request.ReissueRequest;
import com.house.houseviewing.domain.auth.dto.response.LoginResponse;
import com.house.houseviewing.domain.auth.dto.response.ReissueResponse;
import com.house.houseviewing.domain.auth.jwt.JwtTokenProvider;
import com.house.houseviewing.domain.auth.model.CustomUserDetails;
import com.house.houseviewing.domain.auth.service.AuthService;
import com.house.houseviewing.domain.auth.service.RefreshTokenService;
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
            given(refreshTokenService.hashDeviceId("device-1")).willReturn("device-hash-1");
            given(jwtTokenProvider.createAccessToken(anyLong(), anyString(), anyString())).willReturn("access-token");
            given(jwtTokenProvider.createRefreshToken(anyLong(), anyString(), anyString(), anyString())).willReturn("refresh-token");
            given(jwtTokenProvider.getTokenId("refresh-token")).willReturn("refresh-jti-1");
            given(jwtTokenProvider.getRefreshTokenExpiration()).willReturn(604800000L);

            LoginResponse result = authService.login(request, "device-1");

            assertThat(result).isNotNull();
            assertThat(result.getAccessToken()).isEqualTo("access-token");
            assertThat(result.getRefreshToken()).isEqualTo("refresh-token");
            assertThat(result.getUserId()).isEqualTo(1L);
            verify(refreshTokenService).replaceSession(
                    eq(1L),
                    eq("device-hash-1"),
                    anyString(),
                    eq("refresh-token"),
                    eq("refresh-jti-1"),
                    eq(604800000L)
            );
        }
    }

    @Nested
    @DisplayName("토큰 갱신")
    class Reissue {

        @Test
        @DisplayName("성공")
        void 성공(){
            ReissueRequest request = AuthFixture.createReissueRequest("refresh-token").build();

            given(jwtTokenProvider.validateRefreshToken("refresh-token")).willReturn(true);
            given(jwtTokenProvider.getUserId("refresh-token")).willReturn(1L);
            given(jwtTokenProvider.getLoginId("refresh-token")).willReturn("yooyoo9191");
            given(jwtTokenProvider.getSessionId("refresh-token")).willReturn("session-1");
            given(jwtTokenProvider.getDeviceIdHash("refresh-token")).willReturn("device-hash-1");
            given(jwtTokenProvider.getTokenId("refresh-token")).willReturn("old-jti");
            given(jwtTokenProvider.getRemainingTime("refresh-token")).willReturn(604000000L);
            given(refreshTokenService.hashDeviceId("device-1")).willReturn("device-hash-1");
            given(jwtTokenProvider.createAccessToken(1L, "yooyoo9191", "session-1")).willReturn("new-access-token");
            given(jwtTokenProvider.createRefreshToken(1L, "yooyoo9191", "session-1", "device-hash-1")).willReturn("new-refresh-token");
            given(jwtTokenProvider.getTokenId("new-refresh-token")).willReturn("new-jti");
            given(jwtTokenProvider.getRefreshTokenExpiration()).willReturn(604800000L);
            given(refreshTokenService.rotateRefreshToken(
                    eq(1L),
                    eq("device-hash-1"),
                    eq("session-1"),
                    eq("old-jti"),
                    eq("refresh-token"),
                    eq("new-jti"),
                    eq("new-refresh-token"),
                    eq(604800000L),
                    eq(604000000L)
            )).willReturn(RefreshTokenService.RotationResult.SUCCESS);

            ReissueResponse result = authService.reissue(request, "device-1");

            assertThat(result).isNotNull();
            assertThat(result.getAccessToken()).isEqualTo("new-access-token");
            assertThat(result.getRefreshToken()).isEqualTo("new-refresh-token");
        }

        @Test
        @DisplayName("실패: 저장된 세션이 없음")
        void 세션_없음(){
            ReissueRequest request = AuthFixture.createReissueRequest("refresh-token").build();

            given(jwtTokenProvider.validateRefreshToken("refresh-token")).willReturn(true);
            given(jwtTokenProvider.getUserId("refresh-token")).willReturn(1L);
            given(jwtTokenProvider.getLoginId("refresh-token")).willReturn("yooyoo9191");
            given(jwtTokenProvider.getSessionId("refresh-token")).willReturn("session-1");
            given(jwtTokenProvider.getDeviceIdHash("refresh-token")).willReturn("device-hash-1");
            given(jwtTokenProvider.getTokenId("refresh-token")).willReturn("old-jti");
            given(jwtTokenProvider.getRemainingTime("refresh-token")).willReturn(604000000L);
            given(refreshTokenService.hashDeviceId("device-1")).willReturn("device-hash-1");
            given(jwtTokenProvider.createAccessToken(1L, "yooyoo9191", "session-1")).willReturn("new-access-token");
            given(jwtTokenProvider.createRefreshToken(1L, "yooyoo9191", "session-1", "device-hash-1")).willReturn("new-refresh-token");
            given(jwtTokenProvider.getTokenId("new-refresh-token")).willReturn("new-jti");
            given(jwtTokenProvider.getRefreshTokenExpiration()).willReturn(604800000L);
            given(refreshTokenService.rotateRefreshToken(anyLong(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyLong(), anyLong()))
                    .willReturn(RefreshTokenService.RotationResult.INVALID);

            assertThatThrownBy(() -> authService.reissue(request, "device-1"))
                    .isInstanceOf(AppException.class)
                    .extracting("exceptionCode")
                    .isEqualTo(ExceptionCode.INVALID_TOKEN);
        }

        @Test
        @DisplayName("실패: 사용 완료된 Refresh Token 재사용")
        void 사용_완료된_토큰_재사용(){
            ReissueRequest request = AuthFixture.createReissueRequest("refresh-token").build();

            given(jwtTokenProvider.validateRefreshToken("refresh-token")).willReturn(true);
            given(jwtTokenProvider.getUserId("refresh-token")).willReturn(1L);
            given(jwtTokenProvider.getLoginId("refresh-token")).willReturn("yooyoo9191");
            given(jwtTokenProvider.getSessionId("refresh-token")).willReturn("session-1");
            given(jwtTokenProvider.getDeviceIdHash("refresh-token")).willReturn("device-hash-1");
            given(jwtTokenProvider.getTokenId("refresh-token")).willReturn("old-jti");
            given(jwtTokenProvider.getRemainingTime("refresh-token")).willReturn(604000000L);
            given(refreshTokenService.hashDeviceId("device-1")).willReturn("device-hash-1");
            given(jwtTokenProvider.createAccessToken(1L, "yooyoo9191", "session-1")).willReturn("new-access-token");
            given(jwtTokenProvider.createRefreshToken(1L, "yooyoo9191", "session-1", "device-hash-1")).willReturn("new-refresh-token");
            given(jwtTokenProvider.getTokenId("new-refresh-token")).willReturn("new-jti");
            given(jwtTokenProvider.getRefreshTokenExpiration()).willReturn(604800000L);
            given(refreshTokenService.rotateRefreshToken(anyLong(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyLong(), anyLong()))
                    .willReturn(RefreshTokenService.RotationResult.REUSED);

            assertThatThrownBy(() -> authService.reissue(request, "device-1"))
                    .isInstanceOf(AppException.class)
                    .extracting("exceptionCode")
                    .isEqualTo(ExceptionCode.TOKEN_REUSE_DETECTED);
        }
    }

    @Nested
    @DisplayName("로그아웃")
    class Logout {

        @Test
        @DisplayName("성공")
        void 성공(){
            String header = "Bearer access-token";

            given(jwtTokenProvider.getUserId(anyString())).willReturn(1L);
            given(jwtTokenProvider.getSessionId("access-token")).willReturn("session-1");
            given(refreshTokenService.hashDeviceId("device-1")).willReturn("device-hash-1");

            authService.logout(header, "device-1");

            verify(refreshTokenService).revokeCurrentSession(1L, "device-hash-1", "session-1");
        }

        @Test
        @DisplayName("실패: 잘못된 헤더")
        void 잘못된_헤더(){
            String header = "InvalidHeader";

            assertThatThrownBy(() -> authService.logout(header, "device-1"))
                    .isInstanceOf(AppException.class)
                    .extracting("exceptionCode")
                    .isEqualTo(ExceptionCode.INVALID_HEADER);
        }

        @Test
        @DisplayName("실패: null 헤더")
        void null_헤더(){
            assertThatThrownBy(() -> authService.logout(null, "device-1"))
                    .isInstanceOf(AppException.class)
                    .extracting("exceptionCode")
                    .isEqualTo(ExceptionCode.INVALID_HEADER);
        }

        @Test
        @DisplayName("성공: Bearer 토큰")
        void 성공_베어러(){
            String header = "Bearer access-token";

            given(jwtTokenProvider.getUserId(anyString())).willReturn(1L);
            given(jwtTokenProvider.getSessionId("access-token")).willReturn("session-1");
            given(refreshTokenService.hashDeviceId("device-1")).willReturn("device-hash-1");

            authService.logout(header, "device-1");

            verify(refreshTokenService).revokeCurrentSession(1L, "device-hash-1", "session-1");
        }
    }
}
