package com.house.houseviewing.domain.contract.entity;

import com.house.houseviewing.domain.common.BaseTimeEntity;
import com.house.houseviewing.domain.contract.enums.ContractType;
import com.house.houseviewing.domain.house.entity.HouseEntity;
import com.house.houseviewing.domain.analysis.postanalysis.entity.PostAnalysisEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "contracts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ContractEntity extends BaseTimeEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "contract_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "house_id")
    private HouseEntity house;

    @OneToMany(mappedBy = "contract", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PostAnalysisEntity> analyses = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ContractType contractType;

    @Column(nullable = false)
    private Long deposit;

    private Long monthlyAmount;

    @Column(nullable = false)
    private Long maintenanceFee;

    @Column(nullable = false)
    private LocalDate moveDate;

    @Column(nullable = false)
    private LocalDate confirmDate;

    @Builder
    public ContractEntity(Long id, ContractType contractType, Long deposit, Long monthlyAmount, Long maintenanceFee, LocalDate moveDate, LocalDate confirmDate) {
        this.id = id;
        this.contractType = contractType;
        this.deposit = deposit;
        this.monthlyAmount = monthlyAmount;
        this.maintenanceFee = maintenanceFee;
        this.moveDate = moveDate;
        this.confirmDate = confirmDate;
    }

    public void addHouse(HouseEntity house){
        this.house = house;
    }
    public void addRegistryAnalysis(PostAnalysisEntity registryAnalysis){
        this.analyses.add(registryAnalysis);
    }
}
