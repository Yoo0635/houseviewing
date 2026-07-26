package com.house.houseviewing.domain.auth;

import com.house.houseviewing.domain.auth.service.TokenBlacklistService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class TokenBlacklistServiceTest {

    @InjectMocks TokenBlacklistService tokenBlacklistService;

    @Mock StringRedisTemplate stringRedisTemplate;
    @Mock ValueOperations<String, String> valueOperations;

    @Nested
    @DisplayName("토큰 블랙리스트 등록")
    class BlacklistToken {

        @Test
        @DisplayName("성공")
        void 성공(){
            given(stringRedisTemplate.opsForValue()).willReturn(valueOperations);

            tokenBlacklistService.blacklistToken("access-token", 3600000L);

            verify(valueOperations).set(
                    eq("blacklist: access-token"),
                    eq("logout"),
                    eq(3600000L),
                    eq(TimeUnit.MILLISECONDS)
            );
        }
    }

    @Nested
    @DisplayName("블랙리스트 여부 확인")
    class IsBlacklisted {

        @Test
        @DisplayName("블랙리스트됨")
        void 블랙리스트(){
            given(stringRedisTemplate.hasKey("blacklist: access-token")).willReturn(true);

            boolean result = tokenBlacklistService.isBlacklisted("access-token");

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("블랙리스트되지 않음")
        void 미블랙리스트(){
            given(stringRedisTemplate.hasKey("blacklist: access-token")).willReturn(false);

            boolean result = tokenBlacklistService.isBlacklisted("access-token");

            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("키 생성")
    class GenerateKey {

        @Test
        @DisplayName("올바른 키 형식")
        void 올바른_키(){
            String result = tokenBlacklistService.generateKey("test-token");

            assertThat(result).isEqualTo("blacklist: test-token");
        }
    }
}
