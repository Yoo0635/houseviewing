package com.house.houseviewing.domain.auth;

import com.house.houseviewing.domain.auth.service.RefreshTokenService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @InjectMocks RefreshTokenService refreshTokenService;

    @Mock StringRedisTemplate stringRedisTemplate;
    @Mock ValueOperations<String, String> valueOperations;

    @Nested
    @DisplayName("리프레시 토큰 저장")
    class SaveRefreshToken {

        @Test
        @DisplayName("성공")
        void 성공(){
            given(stringRedisTemplate.opsForValue()).willReturn(valueOperations);

            refreshTokenService.saveRefreshToken(1L, "refresh-token", 604800000L);

            verify(valueOperations).set(
                    eq("refresh:1"),
                    eq("refresh-token"),
                    eq(Duration.ofMillis(604800000L))
            );
        }
    }

    @Nested
    @DisplayName("리프레시 토큰 조회")
    class GetRefreshToken {

        @Test
        @DisplayName("성공")
        void 성공(){
            given(stringRedisTemplate.opsForValue()).willReturn(valueOperations);
            given(valueOperations.get("refresh:1")).willReturn("stored-token");

            String result = refreshTokenService.getRefreshToken(1L);

            assertThat(result).isEqualTo("stored-token");
        }

        @Test
        @DisplayName("토큰이 없음")
        void 토큰_없음(){
            given(stringRedisTemplate.opsForValue()).willReturn(valueOperations);
            given(valueOperations.get("refresh:1")).willReturn(null);

            String result = refreshTokenService.getRefreshToken(1L);

            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("리프레시 토큰 삭제")
    class DeleteRefreshToken {

        @Test
        @DisplayName("성공")
        void 성공(){
            refreshTokenService.deleteRefreshToken(1L);

            verify(stringRedisTemplate).delete("refresh:1");
        }
    }
}
