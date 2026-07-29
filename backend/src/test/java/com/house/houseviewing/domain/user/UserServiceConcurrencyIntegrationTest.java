package com.house.houseviewing.domain.user;

import com.house.houseviewing.domain.user.dto.request.UserRegisterRequest;
import com.house.houseviewing.domain.user.repository.UserRepository;
import com.house.houseviewing.domain.user.service.UserService;
import com.house.houseviewing.global.exception.AppException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.datasource.hikari.maximum-pool-size=10",
        "spring.datasource.hikari.minimum-idle=10"
})
@Testcontainers
class UserServiceConcurrencyIntegrationTest {

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("house_viewing")
            .withUsername("root")
            .withPassword("1234");

    @Autowired UserService userService;
    @Autowired UserRepository userRepository;

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
    }

    @AfterEach
    void cleanUp() {
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("동일 이메일과 아이디 회원가입 100건 중 1건만 저장")
    void concurrent_signup_persists_only_one_user() throws Exception {
        int requestCount = 100;
        CountDownLatch ready = new CountDownLatch(requestCount);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger conflictCount = new AtomicInteger();

        ExecutorService executor = Executors.newFixedThreadPool(requestCount);
        try {
            List<Callable<Void>> tasks = new ArrayList<>();
            for (int i = 0; i < requestCount; i++) {
                tasks.add(() -> {
                    ready.countDown();
                    start.await(5, TimeUnit.SECONDS);
                    try {
                        userService.register(registerRequest());
                        successCount.incrementAndGet();
                    } catch (AppException exception) {
                        conflictCount.incrementAndGet();
                    }
                    return null;
                });
            }

            var futures = tasks.stream().map(executor::submit).toList();
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            for (var future : futures) {
                future.get(10, TimeUnit.SECONDS);
            }
        } finally {
            executor.shutdownNow();
        }

        assertThat(successCount.get()).isEqualTo(1);
        assertThat(conflictCount.get()).isEqualTo(99);
        assertThat(userRepository.count()).isEqualTo(1);
        assertThat(userRepository.existsByLoginId("concurrent-user")).isTrue();
    }

    private UserRegisterRequest registerRequest() {
        return UserRegisterRequest.builder()
                .name("concurrent")
                .email("concurrent@example.com")
                .loginId("concurrent-user")
                .password("password123")
                .build();
    }
}
