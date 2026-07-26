package com.house.houseviewing.domain.subscription;

import com.house.houseviewing.domain.subscription.entity.SubscriptionEntity;
import com.house.houseviewing.domain.subscription.enums.PlanType;
import com.house.houseviewing.domain.subscription.repository.SubscriptionRepository;
import com.house.houseviewing.domain.subscription.service.SubscriptionService;
import com.house.houseviewing.domain.user.entity.UserEntity;
import com.house.houseviewing.domain.user.repository.UserRepository;
import com.house.houseviewing.fixture.SubscriptionFixture;
import com.house.houseviewing.fixture.UserFixture;
import com.house.houseviewing.global.exception.AppException;
import com.house.houseviewing.global.exception.ExceptionCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceTest {

    @InjectMocks SubscriptionService subscriptionService;

    @Mock SubscriptionRepository subscriptionRepository;
    @Mock UserRepository userRepository;

    @Nested
    @DisplayName("프리미엄 구독권")
    class Premium{

        @Test
        @DisplayName("성공")
        void 성공(){
            UserEntity user = UserFixture.createDefault().build();
            SubscriptionEntity subscription = SubscriptionFixture.createDefault(user).build();
            user.updateSubscription(subscription);

            given(userRepository.findById(anyLong()))
                    .willReturn(Optional.of(user));

            subscriptionService.premium(1L);

            assertThat(user.getSubscription().getPlanType()).isEqualTo(PlanType.PREMIUM);
        }

        @Test
        @DisplayName("실패: 유저를 찾을 수 없음")
        void 실패(){
            given(userRepository.findById(anyLong()))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> subscriptionService.premium(1L))
                    .isInstanceOf(AppException.class)
                    .extracting("exceptionCode")
                    .isEqualTo(ExceptionCode.USER_NOT_FOUND);
        }
    }

}