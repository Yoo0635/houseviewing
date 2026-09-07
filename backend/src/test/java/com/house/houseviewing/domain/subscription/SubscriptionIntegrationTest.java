package com.house.houseviewing.domain.subscription;

import com.house.houseviewing.domain.subscription.entity.SubscriptionEntity;
import com.house.houseviewing.domain.subscription.enums.FreeDiagnosisStage;
import com.house.houseviewing.domain.subscription.enums.FreeDiagnosisStatus;
import com.house.houseviewing.domain.subscription.enums.PlanType;
import com.house.houseviewing.domain.subscription.repository.SubscriptionRepository;
import com.house.houseviewing.domain.subscription.service.SubscriptionService;
import com.house.houseviewing.domain.user.entity.UserEntity;
import com.house.houseviewing.domain.user.dto.request.UserRegisterRequest;
import com.house.houseviewing.domain.user.repository.UserRepository;
import com.house.houseviewing.domain.user.service.UserService;
import com.house.houseviewing.fixture.UserFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
public class SubscriptionIntegrationTest {

    @Autowired SubscriptionService subscriptionService;
    @Autowired SubscriptionRepository subscriptionRepository;
    @Autowired UserService userService;
    @Autowired UserRepository userRepository;

    @Test
    @DisplayName("무료 구독권")
    void 무료_구둑권(){
        UserEntity user = getUserEntity();
        SubscriptionEntity subscription = user.getSubscription();
        assertThat(subscription.getPlanType()).isEqualTo(PlanType.FREE);
        assertThat(subscription.getUser()).isEqualTo(user);
    }

    @Test
    @DisplayName("프리미엄 구독권")
    void 프리미엄_구독권(){
        UserEntity user = getUserEntity();
        subscriptionService.premium(user.getId());
        assertThat(user.getSubscription().getPlanType()).isEqualTo(PlanType.PREMIUM);
    }

    @Test
    @DisplayName("무료 진단 claim은 첫 요청만 처리 중으로 전환")
    void 무료_진단_claim(){
        UserEntity user = getUserEntity();
        String requestId = "7c7f9b06-f096-48eb-bfa7-09cbe9d1bc93";
        String nextRequestId = "fbad28cf-4e40-4485-97df-eb93fc6196c3";
        LocalDateTime now = LocalDateTime.now();

        int first = subscriptionRepository.claimAvailable(user.getId(), requestId, now.minusMinutes(1));
        int second = subscriptionRepository.claimAvailable(user.getId(), nextRequestId, now.plusMinutes(10));
        int expired = subscriptionRepository.claimExpiredAsNew(user.getId(), nextRequestId, now.plusMinutes(10), now);

        SubscriptionEntity subscription = subscriptionRepository.findByUserId(user.getId()).orElseThrow();
        assertThat(first).isEqualTo(1);
        assertThat(second).isZero();
        assertThat(expired).isEqualTo(1);
        assertThat(subscription.getFreeDiagnosisStatus()).isEqualTo(FreeDiagnosisStatus.PROCESSING);
        assertThat(subscription.getFreeDiagnosisStage()).isEqualTo(FreeDiagnosisStage.ADDRESS);
        assertThat(subscription.getFreeDiagnosisRequestId()).isEqualTo(nextRequestId);
    }

    private UserEntity getUserEntity() {
        UserEntity build = UserFixture.createDefault().build();
        UserRegisterRequest build1 = UserFixture.createRegister(build).build();
        userService.register(build1);
        return userRepository.findByLoginId(build.getLoginId()).orElseThrow();
    }
}
