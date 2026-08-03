package com.house.houseviewing.api.query;

import com.house.houseviewing.api.query.service.DiffDiagnosisLockService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class DiffDiagnosisLockServiceTest {

    @Mock StringRedisTemplate stringRedisTemplate;
    @Mock ValueOperations<String, String> valueOperations;

    @Test
    @DisplayName("같은 스냅샷 문자열은 같은 SHA-256 해시를 생성")
    void same_snapshot_hash_is_stable() {
        DiffDiagnosisLockService service = new DiffDiagnosisLockService(stringRedisTemplate);

        String first = service.createSnapshotHash("{\"status\":\"same\"}");
        String second = service.createSnapshotHash("{\"status\":\"same\"}");

        assertThat(first).isEqualTo(second);
        assertThat(first).hasSize(64);
    }

    @Test
    @DisplayName("Redis SETNX 성공 시 락 소유자 토큰을 반환")
    void try_lock_returns_owner_token_when_setnx_succeeds() {
        given(stringRedisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.setIfAbsent(eq("diff-diagnosis:lock:1:hash-1"), anyString(), any(Duration.class)))
                .willReturn(true);
        DiffDiagnosisLockService service = new DiffDiagnosisLockService(stringRedisTemplate);

        String token = service.tryLock(1L, "hash-1");

        assertThat(token).isNotBlank();
    }

    @Test
    @DisplayName("Redis SETNX 실패 시 null 반환")
    void try_lock_returns_null_when_lock_exists() {
        given(stringRedisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.setIfAbsent(eq("diff-diagnosis:lock:1:hash-1"), anyString(), any(Duration.class)))
                .willReturn(false);
        DiffDiagnosisLockService service = new DiffDiagnosisLockService(stringRedisTemplate);

        String token = service.tryLock(1L, "hash-1");

        assertThat(token).isNull();
    }

    @Test
    @DisplayName("락 해제는 소유자 토큰 비교 Lua 스크립트로 원자 처리")
    void release_uses_compare_and_delete_script() {
        DiffDiagnosisLockService service = new DiffDiagnosisLockService(stringRedisTemplate);

        service.release(1L, "hash-1", "token-1");

        verify(stringRedisTemplate).execute(
                any(RedisScript.class),
                eq(java.util.List.of("diff-diagnosis:lock:1:hash-1")),
                eq("token-1")
        );
    }
}
