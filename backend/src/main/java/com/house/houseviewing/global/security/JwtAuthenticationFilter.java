package com.house.houseviewing.global.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.house.houseviewing.domain.auth.service.CustomUserDetailsService;
import com.house.houseviewing.domain.auth.jwt.JwtTokenProvider;
import com.house.houseviewing.domain.auth.service.TokenBlacklistService;
import com.house.houseviewing.global.exception.ExceptionCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;

import static reactor.netty.http.HttpConnectionLiveness.log;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final CustomUserDetailsService customUserDetailsService;
    private final TokenBlacklistService tokenBlacklistService;
    private final ObjectMapper objectMapper;


    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authorizationHeader = request.getHeader("Authorization");
        String token = null;
        String loginId = null;

        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")){
            token = authorizationHeader.substring(7);

            try {
                if(tokenBlacklistService.isBlacklisted(token)){
                    writeErrorResponse(response, ExceptionCode.INVALID_TOKEN);
                    return;
                }

                jwtTokenProvider.validateToken(token);
                loginId = jwtTokenProvider.getLoginId(token);
            } catch (Exception e){
                log.error("Token validation failed: ", e);
                writeErrorResponse(response, ExceptionCode.INVALID_TOKEN);
                return;
            }
        }

        if (loginId != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = customUserDetailsService.loadUserByUsername(loginId);

            UsernamePasswordAuthenticationToken authenticationToken =
                    new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );
            authenticationToken.setDetails(
                    new WebAuthenticationDetailsSource().buildDetails(request)
            );
            SecurityContextHolder.getContext().setAuthentication(authenticationToken);
        }

        filterChain.doFilter(request, response);
    }

    private void writeErrorResponse(HttpServletResponse response, ExceptionCode exceptionCode) throws IOException {
        response.setStatus(exceptionCode.getStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        Map<String, Object> body = Map.of( "code", exceptionCode.getCode(),
                "message", exceptionCode.getMessage());

        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
