package com.house.houseviewing.domain.report.prereport.entity;

import com.house.houseviewing.domain.analysis.preanalysis.entity.PreAnalysisEntity;
import com.house.houseviewing.domain.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "pre_reports")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PreReportEntity extends BaseTimeEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "report_id")
    private Long id;

    @OneToOne
    @JoinColumn(name = "analysis_id")
    private PreAnalysisEntity analysis;

    @Column(nullable = false)
    private String pdfKey;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String pdfPath;

    @Column(nullable = false)
    private Long pdfSizeBytes;

    @Column(nullable = false)
    private String pdfName;

    @Builder
    public PreReportEntity(Long id, String pdfKey, String pdfPath, Long pdfSizeBytes, String pdfName) {
        this.id = id;
        this.pdfKey = pdfKey;
        this.pdfPath = pdfPath;
        this.pdfSizeBytes = pdfSizeBytes;
        this.pdfName = pdfName;
    }

    public void addAnalysis(PreAnalysisEntity analysis){
        this.analysis = analysis;
        analysis.addReport(this);
    }
}
