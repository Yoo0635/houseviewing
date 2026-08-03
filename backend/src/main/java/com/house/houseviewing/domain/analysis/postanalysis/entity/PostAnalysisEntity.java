package com.house.houseviewing.domain.analysis.postanalysis.entity;

import com.house.houseviewing.domain.analysis.postanalysis.enums.AnalysisType;
import com.house.houseviewing.domain.common.BaseTimeEntity;
import com.house.houseviewing.domain.common.RiskLevel;
import com.house.houseviewing.domain.contract.entity.ContractEntity;
import com.house.houseviewing.domain.house.entity.HouseEntity;
import com.house.houseviewing.domain.report.postreport.entity.PostReportEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "post_analyses",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_post_analysis_diff_snapshot",
                        columnNames = {"house_id", "analysis_type", "snapshot_hash"}
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostAnalysisEntity extends BaseTimeEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "analysis_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "house_id")
    private HouseEntity house;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_id")
    private ContractEntity contract;

    @OneToOne(mappedBy = "analysis", cascade = CascadeType.ALL, orphanRemoval = true)
    private PostReportEntity pdfReport;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RiskLevel riskLevel;

    @Enumerated(EnumType.STRING)
    private AnalysisType analysisType;

    @Column(name = "snapshot_hash", length = 64)
    private String snapshotHash;

    @Column(nullable = false, columnDefinition = "json")
    private String rawData;

    @Column(nullable = false)
    private String mainReason;

    @Column(nullable = false)
    private Integer ltvScore;

    @Builder
    public PostAnalysisEntity(Long id, RiskLevel riskLevel, AnalysisType analysisType, String snapshotHash, String mainReason, Integer ltvScore, String rawData) {
        this.id = id;
        this.riskLevel = riskLevel;
        this.analysisType = analysisType;
        this.snapshotHash = snapshotHash;
        this.mainReason = mainReason;
        this.ltvScore = ltvScore;
        this.rawData = rawData;
    }

    public void addContract(ContractEntity contract){
        this.contract = contract;
        contract.addRegistryAnalysis(this);
    }
    public void addHouse(HouseEntity house){
        this.house = house;
        house.addAnalysis(this);
    }
    public void addPdfReport(PostReportEntity pdfReport) {this.pdfReport = pdfReport;}

    public void assignSnapshotHash(String snapshotHash) {
        this.snapshotHash = snapshotHash;
    }
}
