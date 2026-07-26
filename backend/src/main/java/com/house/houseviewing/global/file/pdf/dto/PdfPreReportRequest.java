package com.house.houseviewing.global.file.pdf.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@Builder
@Getter
@NoArgsConstructor
public class PdfPreReportRequest {

    private String snapshotName;

    private String rawData;
}
