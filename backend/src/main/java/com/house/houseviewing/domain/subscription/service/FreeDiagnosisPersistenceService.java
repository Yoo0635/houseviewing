package com.house.houseviewing.domain.subscription.service;

import com.house.houseviewing.domain.subscription.entity.SubscriptionEntity;
import com.house.houseviewing.domain.subscription.enums.FreeDiagnosisStage;
import com.house.houseviewing.domain.subscription.enums.FreeDiagnosisStatus;
import com.house.houseviewing.domain.subscription.enums.PlanType;
import com.house.houseviewing.domain.subscription.repository.SubscriptionRepository;
import com.house.houseviewing.global.exception.AppException;
import com.house.houseviewing.global.exception.ExceptionCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Service
@RequiredArgsConstructor
public class FreeDiagnosisPersistenceService {

    private static final long LEASE_MINUTES = 10;

    private final SubscriptionRepository subscriptionRepository;
    private final Clock clock;

    @Transactional
    public FreeDiagnosisClaim claim(Long userId, String requestId) {
        LocalDateTime now = LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
        LocalDateTime leaseExpiresAt = now.plusMinutes(LEASE_MINUTES);
        if (subscriptionRepository.claimAvailable(userId, requestId, leaseExpiresAt) == 1) {
            return new FreeDiagnosisClaim(true, false, FreeDiagnosisStatus.PROCESSING, FreeDiagnosisStage.ADDRESS);
        }
        if (subscriptionRepository.claimExpiredAsNew(userId, requestId, leaseExpiresAt, now) == 1) {
            return new FreeDiagnosisClaim(true, false, FreeDiagnosisStatus.PROCESSING, FreeDiagnosisStage.ADDRESS);
        }

        SubscriptionEntity subscription = subscriptionRepository.findByUserId(userId)
                .orElseThrow(() -> new AppException(ExceptionCode.SUBSCRIPTION_NOT_FOUND));
        if (subscription.getPlanType() == PlanType.PREMIUM) {
            return new FreeDiagnosisClaim(true, true, null, null);
        }
        if (subscription.getFreeDiagnosisStatus() == FreeDiagnosisStatus.FAILED) {
            if (requestId.equals(subscription.getFreeDiagnosisRequestId())
                    && subscriptionRepository.resumeFailed(userId, requestId, leaseExpiresAt) == 1) {
                return new FreeDiagnosisClaim(true, false, FreeDiagnosisStatus.PROCESSING, subscription.getFreeDiagnosisStage());
            }
            if (subscriptionRepository.claimFailedAsNew(userId, requestId, leaseExpiresAt) == 1) {
                return new FreeDiagnosisClaim(true, false, FreeDiagnosisStatus.PROCESSING, FreeDiagnosisStage.ADDRESS);
            }
        }
        if (requestId.equals(subscription.getFreeDiagnosisRequestId())) {
            return new FreeDiagnosisClaim(false, false, subscription.getFreeDiagnosisStatus(), subscription.getFreeDiagnosisStage());
        }
        if (subscription.getFreeDiagnosisStatus() == FreeDiagnosisStatus.COMPLETED) {
            throw new AppException(ExceptionCode.FREE_DIAGNOSIS_ALREADY_USED);
        }
        throw new AppException(ExceptionCode.FREE_DIAGNOSIS_IN_PROGRESS);
    }

    @Transactional
    public void updateStage(Long userId, String requestId, FreeDiagnosisStage stage) {
        if (subscriptionRepository.updateStage(userId, requestId, stage.name()) != 1) {
            throw new AppException(ExceptionCode.FREE_DIAGNOSIS_IN_PROGRESS);
        }
    }

    @Transactional
    public void complete(Long userId, String requestId) {
        if (subscriptionRepository.complete(userId, requestId) != 1) {
            throw new AppException(ExceptionCode.FREE_DIAGNOSIS_IN_PROGRESS);
        }
    }

    @Transactional
    public void fail(Long userId, String requestId) {
        subscriptionRepository.fail(userId, requestId);
    }
}
