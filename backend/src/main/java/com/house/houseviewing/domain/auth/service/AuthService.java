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

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;
    private final AuthenticationManager authenticationManager;

    public LoginResponse login(LoginRequest request, String deviceId){
        validateDeviceId(deviceId);
        Authentication authenticate = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getLoginId(),
                        request.getPassword()
                ));
        CustomUserDetails userDetails = (CustomUserDetails) authenticate.getPrincipal();
        String deviceIdHash = refreshTokenService.hashDeviceId(deviceId);
        String sessionId = UUID.randomUUID().toString();
        String accessToken = jwtTokenProvider.createAccessToken(userDetails.getUserId(), userDetails.getUsername(), sessionId);
        String refreshToken = jwtTokenProvider.createRefreshToken(userDetails.getUserId(), userDetails.getUsername(), sessionId, deviceIdHash);
        String refreshJti = jwtTokenProvider.getTokenId(refreshToken);
        refreshTokenService.replaceSession(
                userDetails.getUserId(),
                deviceIdHash,
                sessionId,
                refreshToken,
                refreshJti,
                jwtTokenProvider.getRefreshTokenExpiration()
        );

        return LoginResponse.from(userDetails, accessToken, refreshToken);
    }

    public ReissueResponse reissue(ReissueRequest request, String deviceId){
        validateDeviceId(deviceId);
        String refreshToken = request.getRefreshToken();

        jwtTokenProvider.validateRefreshToken(refreshToken);

        Long userId = jwtTokenProvider.getUserId(refreshToken);
        String loginId = jwtTokenProvider.getLoginId(refreshToken);
        String sessionId = jwtTokenProvider.getSessionId(refreshToken);
        String deviceIdHash = refreshTokenService.hashDeviceId(deviceId);
        String tokenDeviceIdHash = jwtTokenProvider.getDeviceIdHash(refreshToken);
        String oldJti = jwtTokenProvider.getTokenId(refreshToken);
        long oldRemainingTime = jwtTokenProvider.getRemainingTime(refreshToken);

        if (sessionId == null || oldJti == null || !deviceIdHash.equals(tokenDeviceIdHash)) {
            throw new AppException(ExceptionCode.INVALID_TOKEN);
        }

        String accessToken = jwtTokenProvider.createAccessToken(userId, loginId, sessionId);
        String newRefreshToken = jwtTokenProvider.createRefreshToken(userId, loginId, sessionId, deviceIdHash);
        String newJti = jwtTokenProvider.getTokenId(newRefreshToken);
        RefreshTokenService.RotationResult rotationResult = refreshTokenService.rotateRefreshToken(
                userId,
                deviceIdHash,
                sessionId,
                oldJti,
                refreshToken,
                newJti,
                newRefreshToken,
                jwtTokenProvider.getRefreshTokenExpiration(),
                oldRemainingTime
        );

        if (rotationResult == RefreshTokenService.RotationResult.REUSED) {
            throw new AppException(ExceptionCode.TOKEN_REUSE_DETECTED);
        }
        if (rotationResult != RefreshTokenService.RotationResult.SUCCESS) {
            throw new AppException(ExceptionCode.INVALID_TOKEN);
        }

        return ReissueResponse.builder()
                .accessToken(accessToken)
                .refreshToken(newRefreshToken)
                .build();
    }

    public void logout(String authorizationHeader, String deviceId){
        validateDeviceId(deviceId);
        String accessToken = extractToken(authorizationHeader);
        jwtTokenProvider.validateAccessToken(accessToken);
        Long userId = jwtTokenProvider.getUserId(accessToken);
        String sessionId = jwtTokenProvider.getSessionId(accessToken);
        String deviceIdHash = refreshTokenService.hashDeviceId(deviceId);

        refreshTokenService.revokeCurrentSession(userId, deviceIdHash, sessionId);
    }

    private String extractToken(String authorizationHeader) {
        if(authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")){
            throw new AppException(ExceptionCode.INVALID_HEADER);
        }
        return authorizationHeader.substring(7);
    }

    private void validateDeviceId(String deviceId) {
        if (deviceId == null || deviceId.isBlank()) {
            throw new AppException(ExceptionCode.INVALID_HEADER);
        }
    }
}
