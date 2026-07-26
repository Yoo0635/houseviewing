package com.house.houseviewing.global.file.pdf.dto;

import com.house.houseviewing.domain.contract.enums.ContractType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@AllArgsConstructor
@Builder
@Getter
@NoArgsConstructor
public class PdfPostReportRequest {

    private String rawData;

    private ContractType contractType;

    private Long deposit;

    private Long monthlyAmount;

    private Long maintenanceFee;

    private LocalDate moveDate;

    private LocalDate confirmDate;
}
