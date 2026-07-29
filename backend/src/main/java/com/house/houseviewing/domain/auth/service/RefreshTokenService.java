package com.house.houseviewing.domain.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private static final String SESSION_PREFIX = "auth:refresh-session:";
    private static final String USER_DEVICES_PREFIX = "auth:user-devices:";
    private static final String USED_REFRESH_PREFIX = "auth:used-refresh:";
    private static final String REVOKED_SESSION_PREFIX = "auth:revoked-session:";

    private static final DefaultRedisScript<Long> ROTATE_SCRIPT = new DefaultRedisScript<>("""
            if redis.call('EXISTS', KEYS[2]) == 1 then
                redis.call('DEL', KEYS[1])
                redis.call('SET', KEYS[3], 'revoked', 'PX', ARGV[8])
                return 2
            end

            local sessionId = redis.call('HGET', KEYS[1], 'sessionId')
            local jti = redis.call('HGET', KEYS[1], 'jti')
            local refreshTokenHash = redis.call('HGET', KEYS[1], 'refreshTokenHash')

            if not sessionId or not jti or not refreshTokenHash then
                return 0
            end

            if sessionId ~= ARGV[1] or jti ~= ARGV[2] or refreshTokenHash ~= ARGV[3] then
                return 0
            end

            redis.call('HSET', KEYS[1],
                'sessionId', ARGV[1],
                'jti', ARGV[4],
                'refreshTokenHash', ARGV[5],
                'userId', ARGV[9],
                'deviceIdHash', ARGV[10])
            redis.call('PEXPIRE', KEYS[1], ARGV[6])
            redis.call('SADD', KEYS[4], ARGV[10])
            redis.call('PEXPIRE', KEYS[4], ARGV[6])
            redis.call('SET', KEYS[2], 'used', 'PX', ARGV[7])
            return 1
            """, Long.class);

    private final StringRedisTemplate stringRedisTemplate;

    @Value("${jwt.access-token-expiration:900000}")
    private long accessTokenExpirationMs = 900000L;

    public enum RotationResult {
        SUCCESS,
        INVALID,
        REUSED
    }

    public String hashDeviceId(String deviceId) {
        return sha256(deviceId);
    }

    public String hashRefreshToken(String refreshToken) {
        return sha256(refreshToken);
    }

    public void replaceSession(Long userId,
                               String deviceIdHash,
                               String sessionId,
                               String refreshToken,
                               String refreshJti,
                               long expirationMs) {
        String sessionKey = sessionKey(userId, deviceIdHash);
        Object oldSessionId = stringRedisTemplate.opsForHash().get(sessionKey, "sessionId");
        if (oldSessionId != null && !sessionId.equals(oldSessionId.toString())) {
            revokeSession(oldSessionId.toString());
        }

        Map<Object, Object> value = Map.of(
                "sessionId", sessionId,
                "jti", refreshJti,
                "refreshTokenHash", hashRefreshToken(refreshToken),
                "userId", userId.toString(),
                "deviceIdHash", deviceIdHash
        );
        stringRedisTemplate.opsForHash().putAll(sessionKey, value);
        stringRedisTemplate.expire(sessionKey, Duration.ofMillis(normalizeTtl(expirationMs)));
        stringRedisTemplate.opsForSet().add(userDevicesKey(userId), deviceIdHash);
        stringRedisTemplate.expire(userDevicesKey(userId), Duration.ofMillis(normalizeTtl(expirationMs)));
    }

    public RotationResult rotateRefreshToken(Long userId,
                                             String deviceIdHash,
                                             String sessionId,
                                             String oldJti,
                                             String oldRefreshToken,
                                             String newJti,
                                             String newRefreshToken,
                                             long refreshTokenExpirationMs,
                                             long oldRefreshTokenRemainingMs) {
        Long result = stringRedisTemplate.execute(
                ROTATE_SCRIPT,
                List.of(
                        sessionKey(userId, deviceIdHash),
                        usedRefreshKey(sessionId, oldJti),
                        revokedSessionKey(sessionId),
                        userDevicesKey(userId)
                ),
                sessionId,
                oldJti,
                hashRefreshToken(oldRefreshToken),
                newJti,
                hashRefreshToken(newRefreshToken),
                String.valueOf(normalizeTtl(refreshTokenExpirationMs)),
                String.valueOf(normalizeTtl(oldRefreshTokenRemainingMs)),
                String.valueOf(normalizeTtl(accessTokenExpirationMs)),
                String.valueOf(userId),
                deviceIdHash
        );

        if (Long.valueOf(1L).equals(result)) {
            return RotationResult.SUCCESS;
        }
        if (Long.valueOf(2L).equals(result)) {
            return RotationResult.REUSED;
        }
        return RotationResult.INVALID;
    }

    public void revokeCurrentSession(Long userId, String deviceIdHash, String sessionId) {
        String sessionKey = sessionKey(userId, deviceIdHash);
        Object storedSessionId = stringRedisTemplate.opsForHash().get(sessionKey, "sessionId");
        if (storedSessionId != null && sessionId.equals(storedSessionId.toString())) {
            stringRedisTemplate.delete(sessionKey);
        }
        revokeSession(sessionId);
    }

    public void revokeAllUserSessions(Long userId) {
        String userDevicesKey = userDevicesKey(userId);
        Set<String> deviceHashes = stringRedisTemplate.opsForSet().members(userDevicesKey);
        if (deviceHashes == null || deviceHashes.isEmpty()) {
            return;
        }

        HashOperations<String, Object, Object> hashOperations = stringRedisTemplate.opsForHash();
        for (String deviceHash : deviceHashes) {
            String sessionKey = sessionKey(userId, deviceHash);
            Object sessionId = hashOperations.get(sessionKey, "sessionId");
            stringRedisTemplate.delete(sessionKey);
            if (sessionId != null) {
                revokeSession(sessionId.toString());
            }
        }
        stringRedisTemplate.delete(userDevicesKey);
    }

    public boolean isSessionRevoked(String sessionId) {
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(revokedSessionKey(sessionId)));
    }

    private void revokeSession(String sessionId) {
        stringRedisTemplate.opsForValue().set(
                revokedSessionKey(sessionId),
                "revoked",
                Duration.ofMillis(normalizeTtl(accessTokenExpirationMs))
        );
    }

    private String sessionKey(Long userId, String deviceIdHash) {
        return SESSION_PREFIX + userId + ":" + deviceIdHash;
    }

    private String userDevicesKey(Long userId) {
        return USER_DEVICES_PREFIX + userId;
    }

    private String usedRefreshKey(String sessionId, String refreshJti) {
        return USED_REFRESH_PREFIX + sessionId + ":" + refreshJti;
    }

    private String revokedSessionKey(String sessionId) {
        return REVOKED_SESSION_PREFIX + sessionId;
    }

    private long normalizeTtl(long ttlMs) {
        return Math.max(1L, ttlMs);
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                result.append(String.format("%02x", b));
            }
            return result.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }
}
