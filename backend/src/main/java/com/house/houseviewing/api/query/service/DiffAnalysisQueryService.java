package com.house.houseviewing.api.query.service;

import com.house.houseviewing.domain.analysis.postanalysis.entity.PostAnalysisEntity;
import com.house.houseviewing.domain.analysis.postanalysis.enums.AnalysisType;
import com.house.houseviewing.domain.analysis.postanalysis.repository.PostAnalysisRepository;
import com.house.houseviewing.domain.analysis.postanalysis.service.PostAnalysisService;
import com.house.houseviewing.domain.report.postreport.entity.PostReportEntity;
import com.house.houseviewing.domain.report.postreport.service.PostReportService;
import com.house.houseviewing.global.exception.AppException;
import com.house.houseviewing.global.exception.ExceptionCode;
import com.house.houseviewing.global.file.pdf.dto.PdfDownloadResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class DiffAnalysisQueryService {

    private final PostAnalysisRepository postAnalysisRepository;
    private final PostAnalysisService postAnalysisService;
    private final PostReportService postReportService;

    @Transactional
    public PdfDownloadResponse executeDiffDiagnosis(Long houseId){
        // 단계별 mock 시나리오는 "변동(DIFF) 분석 횟수" 기준으로 진행해야 한다.
        // BASIC 분석까지 같이 세면 첫 알림 캐치에서도 WARNING/DANGER가 나오는 문제가 생긴다.
        long count = postAnalysisRepository.countByHouse_IdAndAnalysisType(houseId, AnalysisType.DIFF);
        String snapshot = readMockJson(count);

        PostAnalysisEntity diffAnalysis = postAnalysisService.diffRegister(houseId, snapshot);
        PostReportEntity pdfReport = postReportService.diffRegister(diffAnalysis);

        return PdfDownloadResponse.builder()
                .pdfReportId(pdfReport.getId())
                .filePath(pdfReport.getPdfPath())
                .build();
    }

    public String readMockJson(long count){
        String fileName;
        if (count == 0) {
            fileName = "SAFE-registry.json";
        } else if (count == 1) {
            fileName = "WARNING-registry.json";
        } else {
            fileName = "DANGER-registry.json";
        }
        try {
            ClassPathResource resource = new ClassPathResource("infrastructure/mock/registry/" + fileName);
            return new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e){
            throw new AppException(ExceptionCode.MOCK_NOT_FOUND);
        }
    }
}
