package com.house.houseviewing.api.query.service;

import com.house.houseviewing.domain.analysis.postanalysis.dto.response.AnalysisResponse;
import com.house.houseviewing.domain.analysis.postanalysis.dto.response.PostContractDiagnosisResponse;
import com.house.houseviewing.domain.analysis.postanalysis.entity.PostAnalysisEntity;
import com.house.houseviewing.domain.analysis.postanalysis.service.PostAnalysisService;
import com.house.houseviewing.domain.analysis.preanalysis.entity.PreAnalysisEntity;
import com.house.houseviewing.domain.analysis.preanalysis.service.PreAnalysisService;
import com.house.houseviewing.domain.analysis.preanalysis.dto.request.PreContractDiagnosisRequest;
import com.house.houseviewing.domain.report.postreport.entity.PostReportEntity;
import com.house.houseviewing.domain.report.postreport.service.PostReportService;
import com.house.houseviewing.domain.report.prereport.entity.PreReportEntity;
import com.house.houseviewing.domain.report.prereport.service.PreReportService;
import com.house.houseviewing.global.exception.AppException;
import com.house.houseviewing.global.file.pdf.dto.PdfDownloadResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AnalysisQueryService {

    private final PostAnalysisService postAnalysisService;
    private final PreAnalysisService preAnalysisService;
    private final PostReportService postReportService;
    private final PreReportService preReportService;

    public PostContractDiagnosisResponse executePostContractDiagnosis(Long houseId, MultipartFile snapshot){
        PostAnalysisEntity analyze = postAnalysisService.postRegister(houseId, snapshot);

        try {
            PostReportEntity pdfReport = postReportService.postRegister(analyze);
            return PostContractDiagnosisResponse.success(analyze, pdfReport);
        } catch (AppException e) {
            return PostContractDiagnosisResponse.pdfFailed(analyze, e);
        }
    }

    public PdfDownloadResponse executePreContractDiagnosis(Long userId, PreContractDiagnosisRequest request, MultipartFile snapshot){
        PreAnalysisEntity analyze = preAnalysisService.preRegister(userId, request, snapshot);
        PreReportEntity pdfReport = preReportService.preRegister(analyze);

        return PdfDownloadResponse.builder()
                .pdfReportId(pdfReport.getId())
                .filePath(pdfReport.getPdfPath())
                .build();
    }

    public List<AnalysisResponse> getAnalyses(Long userId){
        List<AnalysisResponse> postAnalyses = postAnalysisService.getPostAnalyses(userId);
        List<AnalysisResponse> preAnalyses = preAnalysisService.getPreAnalyses(userId);

        List<AnalysisResponse> result = new ArrayList<>();
        result.addAll(postAnalyses);
        result.addAll(preAnalyses);

        return result;
    }

    public List<AnalysisResponse> getDiffAnalyses(Long userId){
        return postAnalysisService.getDiffAnalyses(userId);
    }
}
