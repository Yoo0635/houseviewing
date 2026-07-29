package com.house.houseviewing.domain.auth.jwt;

import com.house.houseviewing.global.exception.AppException;
import com.house.houseviewing.global.exception.ExceptionCode;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.access-token-expiration}")
    private long accessToken;

    @Value("${jwt.refresh-token-expiration}")
    private long refreshToken;

    private Key key;

    private static final String TOKEN_TYPE = "tokenType";
    private static final String ACCESS = "ACCESS";
    private static final String REFRESH = "REFRESH";

    @PostConstruct
    public void init(){
        this.key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    }

    private String createToken(Long userId, String loginId, String tokenType, String sessionId, String deviceIdHash, long expirationTime){
        final Date now = new Date(); // 현재시간
        final Date expiry = new Date(now.getTime() + expirationTime); // 만료시간

        JwtBuilder builder = Jwts.builder()
                .setSubject(loginId)
                .setId(UUID.randomUUID().toString())
                .claim("userId", userId)
                .claim("loginId", loginId)
                .claim(TOKEN_TYPE, tokenType)
                .claim("sessionId", sessionId)
                .setIssuedAt(now) // 토큰 발행된 시간
                .setExpiration(expiry) // 유효기간 설정
                .signWith(key, SignatureAlgorithm.HS256);

        if (deviceIdHash != null) {
            builder.claim("deviceIdHash", deviceIdHash);
        }

        return builder.compact();
    }

    public String createAccessToken(Long userId, String loginId){
        return createAccessToken(userId, loginId, UUID.randomUUID().toString());
    }

    public String createAccessToken(Long userId, String loginId, String sessionId){
        return createToken(userId, loginId, ACCESS, sessionId, null, accessToken);
    }

    public String createRefreshToken(Long userId, String loginId){
        return createRefreshToken(userId, loginId, UUID.randomUUID().toString(), null);
    }

    public String createRefreshToken(Long userId, String loginId, String sessionId, String deviceIdHash){
        return createToken(userId, loginId, REFRESH, sessionId, deviceIdHash, refreshToken);
    }

    public long getAccessTokenExpiration(){
        return accessToken;
    }

    public long getRefreshTokenExpiration(){
        return refreshToken;
    }

    public boolean validateToken(String token){
        try{
            Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token);

            return true;
        } catch (ExpiredJwtException e) {
            throw new AppException(ExceptionCode.UNAUTHORIZED);
        } catch (JwtException | IllegalArgumentException e) {
            throw new AppException(ExceptionCode.INVALID_TOKEN);
        }
    }

    public boolean validateAccessToken(String token) {
        validateToken(token);
        validateTokenType(token, ACCESS);
        return true;
    }

    public boolean validateRefreshToken(String token) {
        validateToken(token);
        validateTokenType(token, REFRESH);
        return true;
    }

    private void validateTokenType(String token, String expectedType) {
        if (!expectedType.equals(getTokenType(token))) {
            throw new AppException(ExceptionCode.INVALID_TOKEN);
        }
    }

    public Long getUserId(String token){
        Claims claims = parseClaims(token);
        return claims.get("userId", Long.class);
    }

    public String getLoginId(String token){
        Claims claims = parseClaims(token);
        return claims.get("loginId", String.class);
    }

    public String getTokenType(String token){
        return parseClaims(token).get(TOKEN_TYPE, String.class);
    }

    public String getTokenId(String token){
        return parseClaims(token).getId();
    }

    public String getSessionId(String token){
        return parseClaims(token).get("sessionId", String.class);
    }

    public String getDeviceIdHash(String token){
        return parseClaims(token).get("deviceIdHash", String.class);
    }

    public Claims parseClaims(String token){
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public Long getRemainingTime(String token){
        Date expiration = parseClaims(token).getExpiration();
        return expiration.getTime() - System.currentTimeMillis();
    }
}
