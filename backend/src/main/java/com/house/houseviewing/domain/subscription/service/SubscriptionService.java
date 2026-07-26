package com.house.houseviewing.domain.subscription.service;

import com.house.houseviewing.domain.subscription.entity.SubscriptionEntity;
import com.house.houseviewing.domain.subscription.enums.PlanType;
import com.house.houseviewing.domain.user.entity.UserEntity;
import com.house.houseviewing.domain.user.repository.UserRepository;
import com.house.houseviewing.global.exception.AppException;
import com.house.houseviewing.global.exception.ExceptionCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class SubscriptionService {

    private final UserRepository userRepository;

    @Transactional
    public void premium(Long userId){

        UserEntity user = userRepository.findById(userId).orElseThrow(() -> new AppException(ExceptionCode.USER_NOT_FOUND));

        SubscriptionEntity subscription = user.getSubscription();
        if (subscription == null){
            throw new AppException(ExceptionCode.SUBSCRIPTION_NOT_FOUND);
        }
        subscription.updatePlanType(PlanType.PREMIUM);
    }
}
