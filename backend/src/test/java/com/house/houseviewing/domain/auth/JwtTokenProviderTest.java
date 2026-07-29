package com.house.houseviewing.domain.auth;

import com.house.houseviewing.domain.auth.jwt.JwtTokenProvider;
import com.house.houseviewing.global.exception.AppException;
import com.house.houseviewing.global.exception.ExceptionCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtTokenProviderTest {

    JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider();
        ReflectionTestUtils.setField(jwtTokenProvider, "secretKey", "test-secret-key-that-is-long-enough-for-hs256");
        ReflectionTestUtils.setField(jwtTokenProvider, "accessToken", 900000L);
        ReflectionTestUtils.setField(jwtTokenProvider, "refreshToken", 604800000L);
        jwtTokenProvider.init();
    }

    @Test
    @DisplayName("Access Token은 ACCESS 타입과 세션 ID를 가진다")
    void access_token_has_access_type_and_session_id() {
        String token = jwtTokenProvider.createAccessToken(1L, "yooyoo9191", "session-1");

        assertThat(jwtTokenProvider.getTokenType(token)).isEqualTo("ACCESS");
        assertThat(jwtTokenProvider.getSessionId(token)).isEqualTo("session-1");
        assertThat(jwtTokenProvider.getTokenId(token)).isNotBlank();
    }

    @Test
    @DisplayName("Refresh Token은 REFRESH 타입과 기기 해시를 가진다")
    void refresh_token_has_refresh_type_and_device_hash() {
        String token = jwtTokenProvider.createRefreshToken(1L, "yooyoo9191", "session-1", "device-hash-1");

        assertThat(jwtTokenProvider.getTokenType(token)).isEqualTo("REFRESH");
        assertThat(jwtTokenProvider.getSessionId(token)).isEqualTo("session-1");
        assertThat(jwtTokenProvider.getDeviceIdHash(token)).isEqualTo("device-hash-1");
        assertThat(jwtTokenProvider.getTokenId(token)).isNotBlank();
    }

    @Test
    @DisplayName("Refresh Token은 Access Token 검증을 통과하지 못한다")
    void refresh_token_is_rejected_as_access_token() {
        String refreshToken = jwtTokenProvider.createRefreshToken(1L, "yooyoo9191", "session-1", "device-hash-1");

        assertThatThrownBy(() -> jwtTokenProvider.validateAccessToken(refreshToken))
                .isInstanceOf(AppException.class)
                .extracting("exceptionCode")
                .isEqualTo(ExceptionCode.INVALID_TOKEN);
    }

    @Test
    @DisplayName("Access Token은 Refresh Token 검증을 통과하지 못한다")
    void access_token_is_rejected_as_refresh_token() {
        String accessToken = jwtTokenProvider.createAccessToken(1L, "yooyoo9191", "session-1");

        assertThatThrownBy(() -> jwtTokenProvider.validateRefreshToken(accessToken))
                .isInstanceOf(AppException.class)
                .extracting("exceptionCode")
                .isEqualTo(ExceptionCode.INVALID_TOKEN);
    }
}
