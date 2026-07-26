package com.house.houseviewing.api.query.controller;

import com.house.houseviewing.api.query.service.AnalysisQueryService;
import com.house.houseviewing.domain.analysis.postanalysis.dto.response.AnalysisResponse;
import com.house.houseviewing.domain.auth.model.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/analyses")
@RequiredArgsConstructor
public class AnalysisQueryController {

    private final AnalysisQueryService analysisQueryService;

    @GetMapping
    public ResponseEntity<List<AnalysisResponse>> getAnalyses(@AuthenticationPrincipal CustomUserDetails userDetails){
        List<AnalysisResponse> result = analysisQueryService.getAnalyses(userDetails.getUserId());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/diff")
    public ResponseEntity<List<AnalysisResponse>> getDiffAnalyses(@AuthenticationPrincipal CustomUserDetails userDetails){
        List<AnalysisResponse> result = analysisQueryService.getDiffAnalyses(userDetails.getUserId());
        return ResponseEntity.ok(result);
    }

}
