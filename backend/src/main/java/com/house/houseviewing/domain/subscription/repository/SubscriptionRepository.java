package com.house.houseviewing.domain.subscription.repository;

import com.house.houseviewing.domain.subscription.entity.SubscriptionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.Optional;

public interface SubscriptionRepository extends JpaRepository<SubscriptionEntity, Long> {

    Optional<SubscriptionEntity> findByUserId(Long userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            update subscriptions
            set free_diagnosis_status = 'PROCESSING',
                free_diagnosis_stage = 'ADDRESS',
                free_diagnosis_request_id = :requestId,
                free_diagnosis_lease_expires_at = :leaseExpiresAt
            where user_id = :userId
              and plan_type = 'FREE'
              and free_diagnosis_status = 'AVAILABLE'
            """, nativeQuery = true)
    int claimAvailable(Long userId, String requestId, LocalDateTime leaseExpiresAt);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            update subscriptions
            set free_diagnosis_stage = 'ADDRESS',
                free_diagnosis_request_id = :requestId,
                free_diagnosis_lease_expires_at = :leaseExpiresAt
            where user_id = :userId
              and plan_type = 'FREE'
              and free_diagnosis_status = 'PROCESSING'
              and free_diagnosis_lease_expires_at < :now
              and (free_diagnosis_request_id is null or free_diagnosis_request_id <> :requestId)
            """, nativeQuery = true)
    int claimExpiredAsNew(Long userId, String requestId, LocalDateTime leaseExpiresAt, LocalDateTime now);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            update subscriptions
            set free_diagnosis_status = 'PROCESSING',
                free_diagnosis_lease_expires_at = :leaseExpiresAt
            where user_id = :userId
              and plan_type = 'FREE'
              and free_diagnosis_status = 'FAILED'
              and free_diagnosis_request_id = :requestId
            """, nativeQuery = true)
    int resumeFailed(Long userId, String requestId, LocalDateTime leaseExpiresAt);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            update subscriptions
            set free_diagnosis_status = 'PROCESSING',
                free_diagnosis_stage = 'ADDRESS',
                free_diagnosis_request_id = :requestId,
                free_diagnosis_lease_expires_at = :leaseExpiresAt
            where user_id = :userId
              and plan_type = 'FREE'
              and free_diagnosis_status = 'FAILED'
              and (free_diagnosis_request_id is null or free_diagnosis_request_id <> :requestId)
            """, nativeQuery = true)
    int claimFailedAsNew(Long userId, String requestId, LocalDateTime leaseExpiresAt);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            update subscriptions
            set free_diagnosis_stage = :stage
            where user_id = :userId
              and plan_type = 'FREE'
              and free_diagnosis_status = 'PROCESSING'
              and free_diagnosis_request_id = :requestId
            """, nativeQuery = true)
    int updateStage(Long userId, String requestId, String stage);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            update subscriptions
            set free_diagnosis_status = 'COMPLETED',
                free_diagnosis_lease_expires_at = null
            where user_id = :userId
              and plan_type = 'FREE'
              and free_diagnosis_status = 'PROCESSING'
              and free_diagnosis_request_id = :requestId
            """, nativeQuery = true)
    int complete(Long userId, String requestId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            update subscriptions
            set free_diagnosis_status = 'FAILED',
                free_diagnosis_lease_expires_at = null
            where user_id = :userId
              and plan_type = 'FREE'
              and free_diagnosis_status = 'PROCESSING'
              and free_diagnosis_request_id = :requestId
            """, nativeQuery = true)
    int fail(Long userId, String requestId);
}
