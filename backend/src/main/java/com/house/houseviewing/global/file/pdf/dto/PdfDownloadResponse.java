package com.house.houseviewing.global.file.pdf.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
public class PdfDownloadResponse {

    private Long pdfReportId;

    private String filePath;

    private String status;

    private String stage;

    private String message;

    private String requestId;
}
