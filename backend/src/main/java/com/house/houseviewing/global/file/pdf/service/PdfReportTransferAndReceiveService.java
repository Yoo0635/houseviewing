package com.house.houseviewing.global.file.pdf.service;

import com.house.houseviewing.global.exception.AppException;
import com.house.houseviewing.global.exception.ExceptionCode;
import com.house.houseviewing.global.file.pdf.dto.PdfDiffReportRequest;
import com.house.houseviewing.global.file.pdf.dto.PdfPostReportRequest;
import com.house.houseviewing.global.file.pdf.dto.PdfPreReportRequest;
import com.house.houseviewing.global.file.pdf.dto.PdfUploadResult;
import com.house.houseviewing.infrastructure.python.PythonEngineClient;
import com.house.houseviewing.infrastructure.s3.S3Service;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PdfReportTransferAndReceiveService {

    private final PythonEngineClient pythonEngineClient;
    private final S3Service s3Service;

    public PdfUploadResult postTransferAndReceive(PdfPostReportRequest request){
        byte[] pdf = pythonEngineClient.postSendDataAndReceivePdf(request).block();

        if(pdf == null || pdf.length == 0){
            throw new AppException(ExceptionCode.PDF_SAVE_FAILED);
        }

        PdfUploadResult uploadPdf = s3Service.pdfUpload(pdf);

        return uploadPdf;
    }

    public PdfUploadResult preTransferAndReceive(PdfPreReportRequest request){
        byte[] pdf = pythonEngineClient.preSendDataAndReceivePdf(request).block();

        if(pdf == null || pdf.length == 0){
            throw new AppException(ExceptionCode.PDF_SAVE_FAILED);
        }

        PdfUploadResult uploadPdf = s3Service.pdfUpload(pdf);

        return uploadPdf;
    }

    public PdfUploadResult diffTransferAndReceive(PdfDiffReportRequest request){
        byte[] pdf = pythonEngineClient.diffSendDataAndReceivePdf(request).block();

        if(pdf == null || pdf.length == 0){
            throw new AppException(ExceptionCode.PDF_SAVE_FAILED);
        }

        PdfUploadResult uploadPdf = s3Service.pdfUpload(pdf);

        return uploadPdf;
    }
}
