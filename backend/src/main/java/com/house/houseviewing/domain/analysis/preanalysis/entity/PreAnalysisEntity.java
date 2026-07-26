package com.house.houseviewing.domain.analysis.preanalysis.entity;

import com.house.houseviewing.domain.common.Address;
import com.house.houseviewing.domain.common.BaseTimeEntity;
import com.house.houseviewing.domain.common.RiskLevel;
import com.house.houseviewing.domain.report.prereport.entity.PreReportEntity;
import com.house.houseviewing.domain.user.entity.UserEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "pre_analyses")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PreAnalysisEntity extends BaseTimeEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "analysis_id")
    private Long id;

    @OneToOne(mappedBy = "analysis", cascade = CascadeType.ALL, orphanRemoval = true)
    private PreReportEntity preReportEntity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private UserEntity user;

    @Column(nullable = false)
    private String nickname;

    @Column(columnDefinition = "json", nullable = false)
    private String rawData;

    @Column(nullable = false)
    private String mainReason;

    @Embedded
    @Column(nullable = false)
    private Address address;

    @Enumerated(value = EnumType.STRING)
    @Column(nullable = false)
    private RiskLevel riskLevel;

    @Column(nullable = false)
    private Integer ltvScore;

    @Builder
    public PreAnalysisEntity(Long id, String nickname, String rawData, String mainReason, Address address, RiskLevel riskLevel, Integer ltvScore) {
        this.id = id;
        this.nickname = nickname;
        this.rawData = rawData;
        this.mainReason = mainReason;
        this.address = address;
        this.riskLevel = riskLevel;
        this.ltvScore = ltvScore;
    }

    public void addReport(PreReportEntity report){
        this.preReportEntity = report;
    }

    public void addUser(UserEntity user){
        this.user = user;
        user.addPreAnalysis(this);
    }
}
