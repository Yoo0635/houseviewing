package com.house.houseviewing.domain.subscription.dto.response;

import com.house.houseviewing.domain.subscription.enums.PlanType;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SubscriptionPremiumResponse {

    private Long userId;

    private Long subscriptionId;

    private PlanType planType;
}
