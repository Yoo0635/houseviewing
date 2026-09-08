package com.house.houseviewing.api.query.service;

import com.house.houseviewing.domain.analysis.postanalysis.dto.response.AnalysisResponse;
import com.house.houseviewing.domain.analysis.postanalysis.dto.response.PostContractDiagnosisResponse;
import com.house.houseviewing.domain.analysis.postanalysis.entity.PostAnalysisEntity;
import com.house.houseviewing.domain.analysis.postanalysis.service.PostAnalysisService;
import com.house.houseviewing.domain.analysis.preanalysis.entity.PreAnalysisEntity;
import com.house.houseviewing.domain.analysis.preanalysis.service.PreAnalysisService;
import com.house.houseviewing.domain.analysis.preanalysis.dto.request.PreContractDiagnosisRequest;
import com.house.houseviewing.domain.common.Address;
import com.house.houseviewing.domain.report.postreport.entity.PostReportEntity;
import com.house.houseviewing.domain.report.postreport.service.PostReportRetryService;
import com.house.houseviewing.domain.report.postreport.service.PostReportService;
import com.house.houseviewing.domain.report.prereport.entity.PreReportEntity;
import com.house.houseviewing.domain.report.prereport.service.PreReportService;
import com.house.houseviewing.domain.subscription.enums.FreeDiagnosisStage;
import com.house.houseviewing.domain.subscription.enums.FreeDiagnosisStatus;
import com.house.houseviewing.domain.subscription.service.FreeDiagnosisClaim;
import com.house.houseviewing.domain.subscription.service.FreeDiagnosisPersistenceService;
import com.house.houseviewing.global.exception.AppException;
import com.house.houseviewing.global.file.pdf.dto.PdfDownloadResponse;
import com.house.houseviewing.global.file.pdf.dto.PdfUploadResult;
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
    private final PostReportRetryService postReportRetryService;
    private final PreReportService preReportService;
    private final FreeDiagnosisPersistenceService freeDiagnosisPersistenceService;

    public PostContractDiagnosisResponse executePostContractDiagnosis(Long houseId, MultipartFile snapshot){
        PostAnalysisEntity analyze = postAnalysisService.postRegister(houseId, snapshot);

        try {
            PostReportEntity pdfReport = postReportService.postRegister(analyze);
            return PostContractDiagnosisResponse.success(analyze, pdfReport);
        } catch (AppException e) {
            postReportRetryService.enqueue(analyze, e);
            return PostContractDiagnosisResponse.pdfFailed(analyze, e);
        }
    }

    public PdfDownloadResponse executePreContractDiagnosis(Long userId, String requestId, PreContractDiagnosisRequest request, MultipartFile snapshot){
        FreeDiagnosisClaim claim = freeDiagnosisPersistenceService.claim(userId, requestId);
        if (claim.premium()) {
            PreAnalysisEntity analyze = preAnalysisService.preRegister(userId, request, snapshot);
            PreReportEntity pdfReport = preReportService.preRegister(analyze);
            return completedResponse(pdfReport, requestId);
        }
        if (!claim.executable()) {
            return existingResponse(userId, requestId, claim);
        }

        try {
            PreAnalysisEntity analyze = claim.stage() == FreeDiagnosisStage.PDF
                    ? preAnalysisService.getByRequestId(userId, requestId)
                    : createPreAnalysis(userId, requestId, request, snapshot);
            freeDiagnosisPersistenceService.updateStage(userId, requestId, FreeDiagnosisStage.PDF);
            PreReportEntity pdfReport = createPreReport(analyze);
            freeDiagnosisPersistenceService.complete(userId, requestId);
            return completedResponse(pdfReport, requestId);
        } catch (RuntimeException e) {
            freeDiagnosisPersistenceService.fail(userId, requestId);
            throw e;
        }
    }

    private PreAnalysisEntity createPreAnalysis(Long userId, String requestId, PreContractDiagnosisRequest request, MultipartFile snapshot) {
        Address address = preAnalysisService.parseAddress(request.getAddress());
        freeDiagnosisPersistenceService.updateStage(userId, requestId, FreeDiagnosisStage.ANALYSIS);
        PreAnalysisEntity analyze = preAnalysisService.analyze(request, address, snapshot);
        return preAnalysisService.save(userId, requestId, analyze);
    }

    private PreReportEntity createPreReport(PreAnalysisEntity analyze) {
        PdfUploadResult uploadResult = preReportService.createPdf(analyze);
        return preReportService.save(analyze, uploadResult);
    }

    private PdfDownloadResponse existingResponse(Long userId, String requestId, FreeDiagnosisClaim claim) {
        if (claim.status() == FreeDiagnosisStatus.COMPLETED) {
            PreReportEntity pdfReport = preAnalysisService.getByRequestId(userId, requestId).getPreReportEntity();
            return completedResponse(pdfReport, requestId);
        }
        return PdfDownloadResponse.builder()
                .status(claim.status().name())
                .stage(claim.stage().name())
                .message("같은 요청의 무료 등기부 진단이 처리 중입니다.")
                .requestId(requestId)
                .build();
    }

    private PdfDownloadResponse completedResponse(PreReportEntity pdfReport, String requestId) {
        if (pdfReport == null) {
            return PdfDownloadResponse.builder()
                    .status(FreeDiagnosisStatus.PROCESSING.name())
                    .message("분석은 완료됐지만 PDF 저장이 아직 완료되지 않았습니다.")
                    .requestId(requestId)
                    .build();
        }
        return PdfDownloadResponse.builder()
                .pdfReportId(pdfReport.getId())
                .filePath(pdfReport.getPdfPath())
                .status(FreeDiagnosisStatus.COMPLETED.name())
                .requestId(requestId)
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
