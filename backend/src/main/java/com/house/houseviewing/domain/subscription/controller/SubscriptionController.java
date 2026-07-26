package com.house.houseviewing.domain.subscription.controller;

import com.house.houseviewing.domain.auth.model.CustomUserDetails;
import com.house.houseviewing.domain.subscription.entity.SubscriptionEntity;
import com.house.houseviewing.domain.subscription.dto.response.SubscriptionPremiumResponse;
import com.house.houseviewing.domain.subscription.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    @PostMapping("/premium")
    public ResponseEntity<Void> premium(@AuthenticationPrincipal CustomUserDetails userDetails){
        subscriptionService.premium(userDetails.getUserId());
        return ResponseEntity.ok().build();
    }
}
