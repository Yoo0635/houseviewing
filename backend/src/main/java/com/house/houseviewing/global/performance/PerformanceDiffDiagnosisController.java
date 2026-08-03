package com.house.houseviewing.global.performance;

import com.house.houseviewing.api.query.service.DiffAnalysisQueryService;
import com.house.houseviewing.global.file.pdf.dto.PdfDownloadResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Profile("performance")
@RestController
@RequiredArgsConstructor
@RequestMapping("/performance")
public class PerformanceDiffDiagnosisController {

    private static final String FIXED_SNAPSHOT = """
            {"registryUniqueNo":"perf-fixed","owner":"performance","status":"same","version":1}
            """;

    private final DiffAnalysisQueryService diffAnalysisQueryService;
    private final PerformanceAnalysisSeedService performanceAnalysisSeedService;

    @PostMapping("/houses/{houseId}/baseline-analysis")
    public ResponseEntity<Void> seedBaselineAnalysis(@PathVariable Long houseId) {
        performanceAnalysisSeedService.seedBaselineAnalysis(houseId);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/houses/{houseId}/change-diagnoses/fixed-snapshot")
    public ResponseEntity<PdfDownloadResponse> diffDiagnoseFixedSnapshot(@PathVariable Long houseId) {
        PdfDownloadResponse result = diffAnalysisQueryService.executeDiffDiagnosis(houseId, FIXED_SNAPSHOT);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }
}
