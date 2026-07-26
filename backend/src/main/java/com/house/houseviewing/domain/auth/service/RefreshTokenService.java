package com.house.houseviewing.domain.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    public static final String REFRESH_PREFIX = "refresh:";

    private final StringRedisTemplate stringRedisTemplate;

    public void saveRefreshToken(Long userId, String refreshToken, long expirationMs){
        stringRedisTemplate.opsForValue().set(generateKey(userId), refreshToken, Duration.ofMillis(expirationMs));
    }

    public String getRefreshToken(Long userId){
        return stringRedisTemplate.opsForValue().get(generateKey(userId));
    }

    public void deleteRefreshToken(Long userId){
        stringRedisTemplate.delete(generateKey(userId));
    }

    private String generateKey(Long userId){
        return REFRESH_PREFIX + userId;
    }
}
