package com.house.houseviewing.domain.subscription.entity;

import com.house.houseviewing.domain.common.BaseTimeEntity;
import com.house.houseviewing.domain.subscription.enums.FreeDiagnosisStage;
import com.house.houseviewing.domain.subscription.enums.FreeDiagnosisStatus;
import com.house.houseviewing.domain.subscription.enums.PlanType;
import com.house.houseviewing.domain.user.entity.UserEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "subscriptions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SubscriptionEntity extends BaseTimeEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "subscription_id")
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id")
    private UserEntity user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PlanType planType;

    private LocalDateTime purchasedAt;

    private LocalDateTime endedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FreeDiagnosisStatus freeDiagnosisStatus = FreeDiagnosisStatus.AVAILABLE;

    @Enumerated(EnumType.STRING)
    private FreeDiagnosisStage freeDiagnosisStage;

    private String freeDiagnosisRequestId;

    private LocalDateTime freeDiagnosisLeaseExpiresAt;

    @Builder
    public SubscriptionEntity(Long id, PlanType planType, LocalDateTime purchasedAt, LocalDateTime endedAt,
                              FreeDiagnosisStatus freeDiagnosisStatus, FreeDiagnosisStage freeDiagnosisStage,
                              String freeDiagnosisRequestId, LocalDateTime freeDiagnosisLeaseExpiresAt) {
        this.id = id;
        this.planType = planType;
        this.purchasedAt = purchasedAt;
        this.endedAt = endedAt;
        this.freeDiagnosisStatus = freeDiagnosisStatus == null ? FreeDiagnosisStatus.AVAILABLE : freeDiagnosisStatus;
        this.freeDiagnosisStage = freeDiagnosisStage;
        this.freeDiagnosisRequestId = freeDiagnosisRequestId;
        this.freeDiagnosisLeaseExpiresAt = freeDiagnosisLeaseExpiresAt;
    }

    public void addUser(UserEntity user){this.user = user;}
    public void updatePlanType(PlanType planType){
        this.planType = planType;
    }
}
