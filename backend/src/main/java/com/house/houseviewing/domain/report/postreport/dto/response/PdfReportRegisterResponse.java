package com.house.houseviewing.domain.report.postreport.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor @Builder
public class PdfReportRegisterResponse {

    private Long snapshotId;

    private String pdfName;

    private String pdfPath;

}
