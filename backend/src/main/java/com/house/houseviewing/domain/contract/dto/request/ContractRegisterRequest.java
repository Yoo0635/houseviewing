package com.house.houseviewing.domain.contract.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.house.houseviewing.domain.contract.entity.ContractEntity;
import com.house.houseviewing.domain.contract.enums.ContractType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@AllArgsConstructor
@Builder
@NoArgsConstructor
public class ContractRegisterRequest {

    @NotNull(message = "집 ID는 필수입니다.")
    private Long houseId;

    @NotNull(message = "계약 유형을 입력해주세요.")
    private ContractType contractType;

    @NotNull(message = "보증금을 입력해주세요.")
    private Long deposit;

    @NotNull(message = "월세를 입력해주세요(전세는 해당 사항 없음).")
    @PositiveOrZero
    private Long monthlyAmount;

    @NotNull(message = "월 관리비를 입력해주세요.")
    private Long maintenanceFee;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    @NotNull(message = "전입날짜를 입력해주세요.")
    private LocalDate moveDate;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    @NotNull(message = "확정일자를 입력해주세요.")
    private LocalDate confirmDate;

    public ContractEntity toEntity(){
        return ContractEntity.builder()
                .contractType(contractType)
                .deposit(deposit)
                .monthlyAmount(monthlyAmount)
                .maintenanceFee(maintenanceFee)
                .moveDate(moveDate)
                .confirmDate(confirmDate)
                .build();
    }
}
