package com.house.houseviewing.domain.house.entity;

import com.house.houseviewing.domain.analysis.postanalysis.entity.PostAnalysisEntity;
import com.house.houseviewing.domain.common.Address;
import com.house.houseviewing.domain.common.BaseTimeEntity;
import com.house.houseviewing.domain.contract.entity.ContractEntity;
import com.house.houseviewing.domain.user.entity.UserEntity;
import com.house.houseviewing.domain.user.enums.MonitoringStatus;
import com.house.houseviewing.global.exception.AppException;
import com.house.houseviewing.global.exception.ExceptionCode;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "houses")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class HouseEntity extends BaseTimeEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "house_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private UserEntity user;

    @OneToMany(mappedBy = "house", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PostAnalysisEntity> analyses = new ArrayList<>();

    @OneToMany(mappedBy = "house", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ContractEntity> contracts = new ArrayList<>();

    @Column(nullable = false)
    private String nickname;

    @Embedded
    @Column(nullable = false)
    private Address address;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MonitoringStatus monitoringStatus;

    @Builder
    public HouseEntity(Long id, String nickname, Address address, MonitoringStatus monitoringStatus) {
        this.id = id;
        this.nickname = nickname;
        this.address = address;
        this.monitoringStatus = monitoringStatus;
    }

    public void addContract(ContractEntity contract){
        checkContract(contract);
        this.contracts.add(contract);
        contract.addHouse(this);
    }

    public void checkContract(ContractEntity contract) {
        if(this.contracts.contains(contract))
            throw new AppException(ExceptionCode.ALREADY_REGISTERED_CONTRACT);
    }

    public void updateMonitoringStatus(MonitoringStatus monitoringStatus){
        this.monitoringStatus = monitoringStatus;
    }
    public void updateAddress(Address address) {this.address = address;}
    public void updateNickname(String nickname) {this.nickname = nickname;}
    public void addUser(UserEntity user) {this.user = user;}
    public void addAnalysis(PostAnalysisEntity postAnalysis) {this.analyses.add(postAnalysis);}
}
