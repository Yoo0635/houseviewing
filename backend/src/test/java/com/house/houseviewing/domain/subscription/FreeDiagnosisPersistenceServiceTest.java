package com.house.houseviewing.domain.subscription;

import com.house.houseviewing.domain.subscription.entity.SubscriptionEntity;
import com.house.houseviewing.domain.subscription.enums.FreeDiagnosisStage;
import com.house.houseviewing.domain.subscription.enums.FreeDiagnosisStatus;
import com.house.houseviewing.domain.subscription.enums.PlanType;
import com.house.houseviewing.domain.subscription.repository.SubscriptionRepository;
import com.house.houseviewing.domain.subscription.service.FreeDiagnosisClaim;
import com.house.houseviewing.domain.subscription.service.FreeDiagnosisPersistenceService;
import com.house.houseviewing.global.exception.AppException;
import com.house.houseviewing.global.exception.ExceptionCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class FreeDiagnosisPersistenceServiceTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-09-07T09:00:00Z"),
            ZoneOffset.UTC
    );

    @Mock SubscriptionRepository subscriptionRepository;

    @Test
    void available_free_diagnosis_is_claimed_for_address_stage() {
        String requestId = "7c7f9b06-f096-48eb-bfa7-09cbe9d1bc93";
        LocalDateTime leaseExpiresAt = LocalDateTime.ofInstant(CLOCK.instant(), ZoneOffset.UTC).plusMinutes(10);
        given(subscriptionRepository.claimAvailable(1L, requestId, leaseExpiresAt)).willReturn(1);
        FreeDiagnosisPersistenceService service = new FreeDiagnosisPersistenceService(subscriptionRepository, CLOCK);

        FreeDiagnosisClaim claim = service.claim(1L, requestId);

        assertThat(claim.executable()).isTrue();
        assertThat(claim.premium()).isFalse();
        assertThat(claim.status()).isEqualTo(FreeDiagnosisStatus.PROCESSING);
        assertThat(claim.stage()).isEqualTo(FreeDiagnosisStage.ADDRESS);
    }

    @Test
    void expired_processing_can_be_claimed_by_new_request() {
        String requestId = "fbad28cf-4e40-4485-97df-eb93fc6196c3";
        LocalDateTime now = LocalDateTime.ofInstant(CLOCK.instant(), ZoneOffset.UTC);
        LocalDateTime leaseExpiresAt = now.plusMinutes(10);
        given(subscriptionRepository.claimAvailable(1L, requestId, leaseExpiresAt)).willReturn(0);
        given(subscriptionRepository.claimExpiredAsNew(1L, requestId, leaseExpiresAt, now)).willReturn(1);
        FreeDiagnosisPersistenceService service = new FreeDiagnosisPersistenceService(subscriptionRepository, CLOCK);

        FreeDiagnosisClaim claim = service.claim(1L, requestId);

        assertThat(claim.executable()).isTrue();
        assertThat(claim.status()).isEqualTo(FreeDiagnosisStatus.PROCESSING);
        assertThat(claim.stage()).isEqualTo(FreeDiagnosisStage.ADDRESS);
    }

    @Test
    void same_request_in_progress_returns_current_stage_without_execution() {
        String requestId = "7c7f9b06-f096-48eb-bfa7-09cbe9d1bc93";
        SubscriptionEntity subscription = subscription(PlanType.FREE, FreeDiagnosisStatus.PROCESSING,
                FreeDiagnosisStage.ANALYSIS, requestId);
        given(subscriptionRepository.claimAvailable(1L, requestId, LocalDateTime.ofInstant(CLOCK.instant(), ZoneOffset.UTC).plusMinutes(10)))
                .willReturn(0);
        given(subscriptionRepository.findByUserId(1L)).willReturn(Optional.of(subscription));
        FreeDiagnosisPersistenceService service = new FreeDiagnosisPersistenceService(subscriptionRepository, CLOCK);

        FreeDiagnosisClaim claim = service.claim(1L, requestId);

        assertThat(claim.executable()).isFalse();
        assertThat(claim.status()).isEqualTo(FreeDiagnosisStatus.PROCESSING);
        assertThat(claim.stage()).isEqualTo(FreeDiagnosisStage.ANALYSIS);
    }

    @Test
    void different_request_is_rejected_while_free_diagnosis_is_in_progress() {
        SubscriptionEntity subscription = subscription(PlanType.FREE, FreeDiagnosisStatus.PROCESSING,
                FreeDiagnosisStage.PDF, "7c7f9b06-f096-48eb-bfa7-09cbe9d1bc93");
        given(subscriptionRepository.claimAvailable(1L, "fbad28cf-4e40-4485-97df-eb93fc6196c3",
                LocalDateTime.ofInstant(CLOCK.instant(), ZoneOffset.UTC).plusMinutes(10))).willReturn(0);
        given(subscriptionRepository.findByUserId(1L)).willReturn(Optional.of(subscription));
        FreeDiagnosisPersistenceService service = new FreeDiagnosisPersistenceService(subscriptionRepository, CLOCK);

        assertThatThrownBy(() -> service.claim(1L, "fbad28cf-4e40-4485-97df-eb93fc6196c3"))
                .isInstanceOf(AppException.class)
                .extracting("exceptionCode")
                .isEqualTo(ExceptionCode.FREE_DIAGNOSIS_IN_PROGRESS);
    }

    private SubscriptionEntity subscription(
            PlanType planType,
            FreeDiagnosisStatus status,
            FreeDiagnosisStage stage,
            String requestId
    ) {
        return SubscriptionEntity.builder()
                .planType(planType)
                .freeDiagnosisStatus(status)
                .freeDiagnosisStage(stage)
                .freeDiagnosisRequestId(requestId)
                .build();
    }
}
