package com.house.houseviewing.global.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.house.houseviewing.domain.auth.jwt.JwtTokenProvider;
import com.house.houseviewing.domain.auth.service.CustomUserDetailsService;
import com.house.houseviewing.domain.auth.service.TokenBlacklistService;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock JwtTokenProvider jwtTokenProvider;
    @Mock CustomUserDetailsService customUserDetailsService;
    @Mock TokenBlacklistService tokenBlacklistService;
    @Mock FilterChain filterChain;

    @Test
    @DisplayName("토큰 처리 이후 예외는 JWT 에러로 덮어쓰지 않고 전파한다")
    void downstream_exception_is_propagated() throws Exception {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(
                jwtTokenProvider,
                customUserDetailsService,
                tokenBlacklistService,
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
}
