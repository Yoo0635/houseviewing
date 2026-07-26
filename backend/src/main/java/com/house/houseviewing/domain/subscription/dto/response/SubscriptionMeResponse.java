package com.house.houseviewing.domain.subscription.dto.response;

import com.house.houseviewing.domain.subscription.entity.SubscriptionEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor @Builder
public class SubscriptionMeResponse {

    private String planType;

    public static SubscriptionMeResponse from(SubscriptionEntity subscription){
        return SubscriptionMeResponse.builder()
                .planType(subscription.getPlanType().name())
                .build();
    }
}
