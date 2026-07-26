package com.house.houseviewing.domain.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

    private static final String BLACKLIST_PREFIX = "blacklist: ";

    private final StringRedisTemplate stringRedisTemplate;

    public void blacklistToken(String accessToken, long expirationMs){
        String key = generateKey(accessToken);
        stringRedisTemplate.opsForValue().set(key, "logout", expirationMs, TimeUnit.MILLISECONDS);
    }

    public String generateKey(String accessToken){
        return BLACKLIST_PREFIX + accessToken;
    }

    public boolean isBlacklisted(String accessToken){
        String key = generateKey(accessToken);
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(key));
    }
}
