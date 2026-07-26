package com.house.houseviewing.fixture;

import com.house.houseviewing.domain.report.postreport.entity.PostReportEntity;

public class PostReportFixture {

    public static PostReportEntity.PostReportEntityBuilder createDefault(){
        return PostReportEntity.builder()
                .pdfKey("post-key")
                .pdfPath("/test/post/path")
                .pdfSizeBytes(2048L)
                .pdfName("post_test.pdf");
    }
}
