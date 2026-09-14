package com.house.houseviewing.api.query.controller;

import com.house.houseviewing.api.query.dto.AnalysisHistoryPageResponse;
import com.house.houseviewing.api.query.service.AnalysisQueryService;
import com.house.houseviewing.domain.auth.model.CustomUserDetails;
import com.house.houseviewing.domain.common.RiskLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/analyses")
@RequiredArgsConstructor
public class AnalysisQueryController {

    private final AnalysisQueryService analysisQueryService;

    @GetMapping
    public ResponseEntity<AnalysisHistoryPageResponse> getAnalyses(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(defaultValue = "0") long offset,
            @RequestParam(required = false) RiskLevel riskLevel
    ){
        AnalysisHistoryPageResponse result = analysisQueryService.getAnalyses(userDetails.getUserId(), offset, riskLevel);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/diff")
    public ResponseEntity<AnalysisHistoryPageResponse> getDiffAnalyses(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(defaultValue = "0") long offset,
            @RequestParam(required = false) RiskLevel riskLevel
    ){
        AnalysisHistoryPageResponse result = analysisQueryService.getDiffAnalyses(userDetails.getUserId(), offset, riskLevel);
        return ResponseEntity.ok(result);
    }

}
