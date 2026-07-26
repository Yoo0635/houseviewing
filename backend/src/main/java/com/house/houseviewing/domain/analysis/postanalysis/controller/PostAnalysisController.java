package com.house.houseviewing.domain.analysis.postanalysis.controller;

import com.house.houseviewing.api.query.service.AnalysisQueryService;
import com.house.houseviewing.api.query.service.DiffAnalysisQueryService;
import com.house.houseviewing.domain.analysis.postanalysis.dto.response.PostContractDiagnosisResponse;
import com.house.houseviewing.global.file.pdf.dto.PdfDownloadResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/analysis")
public class PostAnalysisController {

    private final AnalysisQueryService analysisQueryService;
    private final DiffAnalysisQueryService diffAnalysisQueryService;

    @PostMapping("/{houseId}/post-contract-diagnoses")
    public ResponseEntity<PostContractDiagnosisResponse> diagnosePostContract(
            @PathVariable Long houseId,
            @RequestPart("file") MultipartFile snapshot){
        PostContractDiagnosisResponse result = analysisQueryService.executePostContractDiagnosis(houseId, snapshot);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/{houseId}/change-diagnoses")
    public ResponseEntity<PdfDownloadResponse> diffDiagnose(@PathVariable Long houseId){
        PdfDownloadResponse result = diffAnalysisQueryService.executeDiffDiagnosis(houseId);
        return ResponseEntity.status(org.springframework.http.HttpStatus.CREATED).body(result);
    }
}
