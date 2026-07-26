package com.house.houseviewing.domain.auth.service;

import com.house.houseviewing.domain.auth.dto.request.LoginRequest;
import com.house.houseviewing.domain.auth.dto.request.ReissueRequest;
import com.house.houseviewing.domain.auth.dto.response.LoginResponse;
import com.house.houseviewing.domain.auth.dto.response.ReissueResponse;
import com.house.houseviewing.domain.auth.jwt.JwtTokenProvider;
import com.house.houseviewing.domain.auth.model.CustomUserDetails;
import com.house.houseviewing.global.exception.AppException;
import com.house.houseviewing.global.exception.ExceptionCode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;
    private final AuthenticationManager authenticationManager;
    private final TokenBlacklistService tokenBlacklistService;

    public LoginResponse login(LoginRequest request){
        Authentication authenticate = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getLoginId(),
                        request.getPassword()
                ));
        CustomUserDetails userDetails = (CustomUserDetails) authenticate.getPrincipal();
        String accessToken = jwtTokenProvider.createAccessToken(userDetails.getUserId(), userDetails.getUsername());
        String refreshToken = jwtTokenProvider.createRefreshToken(userDetails.getUserId(), userDetails.getUsername());
        refreshTokenService.saveRefreshToken(userDetails.getUserId(), refreshToken, jwtTokenProvider.getRefreshTokenExpiration());

        return LoginResponse.from(userDetails, accessToken, refreshToken);
    }

    public ReissueResponse reissue(ReissueRequest request){
        String refreshToken = request.getRefreshToken();

        jwtTokenProvider.validateToken(refreshToken);

        Long userId = jwtTokenProvider.getUserId(refreshToken);
        String loginId = jwtTokenProvider.getLoginId(refreshToken);

        String savedRefreshToken = refreshTokenService.getRefreshToken(userId);

        if(savedRefreshToken == null || !savedRefreshToken.equals(refreshToken)){
            throw new AppException(ExceptionCode.INVALID_TOKEN);
        }
        String accessToken = jwtTokenProvider.createAccessToken(userId, loginId);
        return ReissueResponse.builder().accessToken(accessToken).build();
    }

    public void logout(String authorizationHeader){
        String accessToken = extractToken(authorizationHeader);
        Long remainingTime = jwtTokenProvider.getRemainingTime(accessToken);
        Long userId = jwtTokenProvider.getUserId(accessToken);

        tokenBlacklistService.blacklistToken(accessToken, remainingTime);
        refreshTokenService.deleteRefreshToken(userId);
    }

    private String extractToken(String authorizationHeader) {
        if(authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")){
            throw new AppException(ExceptionCode.INVALID_HEADER);
        }
        return authorizationHeader.substring(7);
    }
}
