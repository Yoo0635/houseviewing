package com.house.houseviewing.domain.subscription.repository;

import com.house.houseviewing.domain.subscription.entity.SubscriptionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubscriptionRepository extends JpaRepository<SubscriptionEntity, Long> {
}
