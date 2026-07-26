package com.house.houseviewing.domain.house.dto.response;

import com.house.houseviewing.domain.contract.entity.ContractEntity;
import com.house.houseviewing.domain.analysis.postanalysis.entity.PostAnalysisEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@NoArgsConstructor @Getter
@AllArgsConstructor @Builder
public class HouseMeResponse {

    private String nickname;

    private String address;

    private Long deposit;

    private Long monthlyAmount;

    private Long maintenanceFee;

    private LocalDate moveDate;

    private LocalDate confirmDate;

    private Integer ltvScore;

    public static HouseMeResponse from(ContractEntity contract, PostAnalysisEntity analysis){

        return HouseMeResponse.builder()
                .nickname(contract.getHouse().getNickname())
                .address(contract.getHouse().getAddress().getAddressName())
                .deposit(contract.getDeposit())
                .monthlyAmount(contract.getMonthlyAmount())
                .maintenanceFee(contract.getMaintenanceFee())
                .moveDate(contract.getMoveDate())
                .confirmDate(contract.getConfirmDate())
                .ltvScore(analysis.getLtvScore())
                .build();
    }
}
