package com.house.houseviewing.fixture;

import com.house.houseviewing.domain.subscription.entity.SubscriptionEntity;
import com.house.houseviewing.domain.subscription.enums.PlanType;
import com.house.houseviewing.domain.user.entity.UserEntity;

public class SubscriptionFixture {

    public static SubscriptionEntity.SubscriptionEntityBuilder createDefault(UserEntity user){
        return SubscriptionEntity.builder()
                .planType(PlanType.FREE);
    }
}
