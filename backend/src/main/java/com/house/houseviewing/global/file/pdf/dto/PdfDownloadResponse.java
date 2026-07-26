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
}
