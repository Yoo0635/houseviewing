package com.house.houseviewing.domain.auth;

import com.house.houseviewing.domain.auth.service.RefreshTokenService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @InjectMocks RefreshTokenService refreshTokenService;

    @Mock StringRedisTemplate stringRedisTemplate;
    @Mock ValueOperations<String, String> valueOperations;
    @Mock HashOperations<String, Object, Object> hashOperations;
    @Mock SetOperations<String, String> setOperations;

    @Test
    @DisplayName("deviceId와 Refresh Token은 SHA-256 해시로 변환한다")
    void hashes_device_id_and_refresh_token() {
        assertThat(refreshTokenService.hashDeviceId("device-1"))
                .isEqualTo("03204de92e11fc8c528139be419065920eb83dbff1a4663bbea455aa6e9702bd");
        assertThat(refreshTokenService.hashRefreshToken("refresh-token"))
                .isEqualTo("0eb17643d4e9261163783a420859c92c7d212fa9624106a12b510afbec266120");
    }

    @Nested
    @DisplayName("기기 세션 교체")
    class ReplaceSession {

        @Test
        @DisplayName("성공: 원문 토큰 대신 해시와 jti를 저장")
        void 성공() {
            given(stringRedisTemplate.opsForHash()).willReturn(hashOperations);
            given(stringRedisTemplate.opsForSet()).willReturn(setOperations);

            refreshTokenService.replaceSession(
                    1L,
                    "device-hash-1",
                    "session-1",
                    "refresh-token",
                    "refresh-jti-1",
                    604800000L
            );

            verify(hashOperations).putAll(
                    eq("auth:refresh-session:1:device-hash-1"),
                    argThat((Map<Object, Object> value) ->
                            value.get("sessionId").equals("session-1")
                                    && value.get("jti").equals("refresh-jti-1")
                                    && value.get("refreshTokenHash").equals(refreshTokenService.hashRefreshToken("refresh-token"))
                                    && !value.containsValue("refresh-token"))
            );
            verify(stringRedisTemplate).expire(
                    eq("auth:refresh-session:1:device-hash-1"),
                    eq(Duration.ofMillis(604800000L))
            );
            verify(setOperations).add("auth:user-devices:1", "device-hash-1");
        }
    }

    @Test
    @DisplayName("Refresh Token 회전은 Redis Lua 스크립트로 원자 처리한다")
    void rotates_refresh_token_with_lua_script() {
        given(stringRedisTemplate.execute(
                any(RedisScript.class),
                anyList(),
                any(Object[].class)
        )).willReturn(1L);

        RefreshTokenService.RotationResult result = refreshTokenService.rotateRefreshToken(
                1L,
                "device-hash-1",
                "session-1",
                "old-jti",
                "old-refresh-token",
                "new-jti",
                "new-refresh-token",
                604800000L,
                604000000L
        );

        assertThat(result).isEqualTo(RefreshTokenService.RotationResult.SUCCESS);
        verify(stringRedisTemplate).execute(
                any(RedisScript.class),
                eq(List.of(
                        "auth:refresh-session:1:device-hash-1",
                        "auth:used-refresh:session-1:old-jti",
                        "auth:revoked-session:session-1",
                        "auth:user-devices:1"
                )),
                any(Object[].class)
        );
    }

    @Nested
    @DisplayName("현재 기기 세션 폐기")
    class RevokeCurrentSession {

        @Test
        @DisplayName("성공")
        void 성공() {
            given(stringRedisTemplate.opsForHash()).willReturn(hashOperations);
            given(hashOperations.get("auth:refresh-session:1:device-hash-1", "sessionId")).willReturn("session-1");
            given(stringRedisTemplate.opsForValue()).willReturn(valueOperations);

            refreshTokenService.revokeCurrentSession(1L, "device-hash-1", "session-1");

            verify(stringRedisTemplate).delete("auth:refresh-session:1:device-hash-1");
            verify(valueOperations).set(
                    eq("auth:revoked-session:session-1"),
                    eq("revoked"),
                    any(Duration.class)
            );
        }
    }
}
