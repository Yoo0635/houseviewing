package com.house.houseviewing.domain.user.entity;

import com.house.houseviewing.domain.analysis.preanalysis.entity.PreAnalysisEntity;
import com.house.houseviewing.domain.common.BaseTimeEntity;
import com.house.houseviewing.domain.house.entity.HouseEntity;
import com.house.houseviewing.domain.subscription.enums.PlanType;
import com.house.houseviewing.domain.subscription.entity.SubscriptionEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserEntity extends BaseTimeEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<HouseEntity> houses = new ArrayList<>();

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private SubscriptionEntity subscription;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PreAnalysisEntity> analyses = new ArrayList<>();

    @Column(nullable = false)
    private String name;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(unique = true, nullable = false)
    private String loginId;

    @Column(nullable = false)
    private String password;

    @Builder
    public UserEntity(Long id, String name, String email, String loginId, String password) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.loginId = loginId;
        this.password = password;
    }

    public void addHouse(HouseEntity house){
        houses.add(house);
        house.addUser(this);
    }

    public void addSubscription(SubscriptionEntity subscription){
        this.subscription = subscription;
        subscription.addUser(this);
    }

    public void addPreAnalysis(PreAnalysisEntity preAnalysisEntity){
        this.analyses.add(preAnalysisEntity);
    }

    public void updateSubscription(SubscriptionEntity subscription){this.subscription = subscription;}
    public void updatePassword(String password){this.password = password;}

    public boolean isPremium() {
        return this.getSubscription().getPlanType() == PlanType.PREMIUM && this.getSubscription() != null;
    }
}
