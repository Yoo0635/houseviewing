package com.house.houseviewing.fixture;

import com.house.houseviewing.domain.report.prereport.entity.PreReportEntity;

public class PreReportFixture {

    public static PreReportEntity.PreReportEntityBuilder createDefault(){
        return PreReportEntity.builder()
                .pdfKey("test-key")
                .pdfPath("/test/path")
                .pdfSizeBytes(1024L)
                .pdfName("test.pdf");
    }
}
