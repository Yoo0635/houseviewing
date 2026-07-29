package com.house.houseviewing.global.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.house.houseviewing.domain.auth.jwt.JwtTokenProvider;
import com.house.houseviewing.domain.auth.service.CustomUserDetailsService;
import com.house.houseviewing.domain.auth.service.RefreshTokenService;
import com.house.houseviewing.domain.auth.service.TokenBlacklistService;
import com.house.houseviewing.global.exception.AppException;
import com.house.houseviewing.global.exception.ExceptionCode;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock JwtTokenProvider jwtTokenProvider;
    @Mock CustomUserDetailsService customUserDetailsService;
    @Mock TokenBlacklistService tokenBlacklistService;
    @Mock RefreshTokenService refreshTokenService;
    @Mock FilterChain filterChain;

    @Test
    @DisplayName("토큰 처리 이후 예외는 JWT 에러로 덮어쓰지 않고 전파한다")
    void downstream_exception_is_propagated() throws Exception {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(
                jwtTokenProvider,
                customUserDetailsService,
                tokenBlacklistService,
                refreshTokenService,
                new ObjectMapper()
        );
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        doThrow(new IllegalStateException("downstream failure"))
                .when(filterChain)
                .doFilter(request, response);

        assertThatThrownBy(() -> filter.doFilter(request, response, filterChain))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("downstream failure");
    }

    @Test
    @DisplayName("Refresh Token이 Authorization에 들어오면 API 인증을 거절한다")
    void refresh_token_is_rejected_for_api_authentication() throws Exception {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(
                jwtTokenProvider,
                customUserDetailsService,
                tokenBlacklistService,
                refreshTokenService,
                new ObjectMapper()
        );
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer refresh-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        doThrow(new AppException(ExceptionCode.INVALID_TOKEN))
                .when(jwtTokenProvider)
                .validateAccessToken("refresh-token");

        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(401);
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    @DisplayName("폐기된 세션의 Access Token은 만료 전에도 인증을 거절한다")
    void revoked_session_access_token_is_rejected_before_expiration() throws Exception {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(
                jwtTokenProvider,
                customUserDetailsService,
                tokenBlacklistService,
                refreshTokenService,
                new ObjectMapper()
        );
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer access-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        given(jwtTokenProvider.validateAccessToken("access-token")).willReturn(true);
        given(jwtTokenProvider.getSessionId("access-token")).willReturn("session-1");
        given(refreshTokenService.isSessionRevoked("session-1")).willReturn(true);

        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(401);
        verify(filterChain, never()).doFilter(request, response);
    }
}
