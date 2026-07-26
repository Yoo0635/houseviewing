package com.house.houseviewing.domain.report.prereport.service;

import com.house.houseviewing.domain.analysis.preanalysis.entity.PreAnalysisEntity;
import com.house.houseviewing.domain.report.prereport.entity.PreReportEntity;
import com.house.houseviewing.domain.report.prereport.repository.PreReportRepository;
import com.house.houseviewing.global.file.pdf.dto.PdfPreReportRequest;
import com.house.houseviewing.global.file.pdf.dto.PdfUploadResult;
import com.house.houseviewing.global.file.pdf.service.PdfReportTransferAndReceiveService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PreReportService {

    private final PreReportRepository preReportRepository;
    private final PdfReportTransferAndReceiveService pdfReportTransferAndReceiveService;

    @Transactional
    public PreReportEntity preRegister(PreAnalysisEntity analyze){
        PdfPreReportRequest request = getPdfReportPreRequest(analyze);
        PdfUploadResult uploadResult = pdfReportTransferAndReceiveService.preTransferAndReceive(request);
        PreReportEntity pdfReport = getPdfReportEntity(uploadResult, analyze);

        return preReportRepository.save(pdfReport);
    }

    private static PdfPreReportRequest getPdfReportPreRequest(PreAnalysisEntity analyze) {
        return PdfPreReportRequest.builder()
                .snapshotName(analyze.getNickname())
                .rawData(analyze.getRawData())
                .build();
    }

    private static PreReportEntity getPdfReportEntity(PdfUploadResult uploadResult, PreAnalysisEntity analyze) {
        PreReportEntity pdfReport = PreReportEntity.builder()
                .pdfName(uploadResult.getPdfName())
                .pdfPath(uploadResult.getPdfPath())
                .pdfKey(uploadResult.getPdfKey())
                .pdfSizeBytes(uploadResult.getPdfSizeBytes())
                .build();
        pdfReport.addAnalysis(analyze);
        return pdfReport;
    }
}
