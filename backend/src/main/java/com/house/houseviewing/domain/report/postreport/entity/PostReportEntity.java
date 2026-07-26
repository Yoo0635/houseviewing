package com.house.houseviewing.domain.report.postreport.entity;

import com.house.houseviewing.domain.common.BaseTimeEntity;
import com.house.houseviewing.domain.analysis.postanalysis.entity.PostAnalysisEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "post_reports")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostReportEntity extends BaseTimeEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "report_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "analysis_id", unique = true)
    private PostAnalysisEntity analysis;

    @Column(nullable = false)
    private String pdfKey;

    @Column(nullable = false)
    private String pdfName;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String pdfPath;

    @Column(nullable = false)
    private Long pdfSizeBytes;

    @Builder
    public PostReportEntity(Long id, String pdfKey, String pdfName, String pdfPath, Long pdfSizeBytes) {
        this.id = id;
        this.pdfKey = pdfKey;
        this.pdfName = pdfName;
        this.pdfPath = pdfPath;
        this.pdfSizeBytes = pdfSizeBytes;
    }

    public void addRegistryAnalysis(PostAnalysisEntity registryAnalysis){
        this.analysis = registryAnalysis;
        registryAnalysis.addPdfReport(this);
    }
}
