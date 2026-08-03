package com.house.houseviewing.api.query.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DiffDiagnosisLockService {

    private static final Duration LOCK_TTL = Duration.ofSeconds(30);
    private static final DefaultRedisScript<Long> RELEASE_SCRIPT = new DefaultRedisScript<>("""
            if redis.call('get', KEYS[1]) == ARGV[1] then
              return redis.call('del', KEYS[1])
            end
            return 0
            """, Long.class);

    private final StringRedisTemplate stringRedisTemplate;

    public String createSnapshotHash(String snapshot) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(snapshot.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm is not available", e);
        }
    }

    public String tryLock(Long houseId, String snapshotHash) {
        String token = UUID.randomUUID().toString();
        Boolean acquired = stringRedisTemplate.opsForValue()
                .setIfAbsent(lockKey(houseId, snapshotHash), token, LOCK_TTL);
        return Boolean.TRUE.equals(acquired) ? token : null;
    }

    public void release(Long houseId, String snapshotHash, String token) {
        if (token == null) {
            return;
        }
        stringRedisTemplate.execute(RELEASE_SCRIPT, List.of(lockKey(houseId, snapshotHash)), token);
    }

    private String lockKey(Long houseId, String snapshotHash) {
        return "diff-diagnosis:lock:%d:%s".formatted(houseId, snapshotHash);
    }
}
