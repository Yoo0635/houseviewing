package com.house.houseviewing.domain.subscription;

import com.house.houseviewing.domain.subscription.entity.SubscriptionEntity;
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

    private UserEntity getUserEntity() {
        UserEntity build = UserFixture.createDefault().build();
        UserRegisterRequest build1 = UserFixture.createRegister(build).build();
        userService.register(build1);
        return userRepository.findByLoginId(build.getLoginId()).orElseThrow();
    }
}
