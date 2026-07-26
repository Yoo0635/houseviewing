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

@Component
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.access-token-expiration}")
    private long accessToken;

    @Value("${jwt.refresh-token-expiration}")
    private long refreshToken;

    private Key key;

    @PostConstruct
    protected void init(){
        this.key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    }

    private String createToken(Long userId, String loginId, long expirationTime){
        final Date now = new Date(); // 현재시간
        final Date expiry = new Date(now.getTime() + expirationTime); // 만료시간

        return Jwts.builder()
                .setSubject(loginId)
                .claim("userId", userId)
                .claim("loginId", loginId)
                .setIssuedAt(now) // 토큰 발행된 시간
                .setExpiration(expiry) // 유효기간 설정
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public String createAccessToken(Long userId, String loginId){
        return createToken(userId, loginId, accessToken);
    }

    public String createRefreshToken(Long userId, String loginId){
        return createToken(userId, loginId, refreshToken);
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

    public Long getUserId(String token){
        Claims claims = parseClaims(token);
        return claims.get("userId", Long.class);
    }

    public String getLoginId(String token){
        Claims claims = parseClaims(token);
        return claims.get("loginId", String.class);
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
