package com.house.houseviewing.domain.analysis.preanalysis.controller;

import com.house.houseviewing.api.query.service.AnalysisQueryService;
import com.house.houseviewing.domain.analysis.preanalysis.dto.request.PreContractDiagnosisRequest;
import com.house.houseviewing.global.file.pdf.dto.PdfDownloadResponse;
import com.house.houseviewing.domain.auth.model.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/analysis")
@RequiredArgsConstructor
public class PreAnalysisController {

    private final AnalysisQueryService analysisQueryService;

    @PostMapping("/pre-contract-diganoses")
    public ResponseEntity<PdfDownloadResponse> diagnosePreContract(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestPart("file") MultipartFile snapshot,
            @RequestPart("data") PreContractDiagnosisRequest request){

        PdfDownloadResponse result = analysisQueryService.executePreContractDiagnosis(userDetails.getUserId(), request, snapshot);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }
}
