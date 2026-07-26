package com.house.houseviewing.domain.report.postreport.controller;

import com.house.houseviewing.domain.report.postreport.service.PostReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/pdf")
@RequiredArgsConstructor
public class PostReportController {

    private final PostReportService postReportService;
}
