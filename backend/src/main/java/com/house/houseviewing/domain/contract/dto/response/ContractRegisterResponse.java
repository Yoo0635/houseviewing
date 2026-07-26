package com.house.houseviewing.domain.contract.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@Builder
@NoArgsConstructor
public class ContractRegisterResponse {

    private Long houseId;

    private Long contractId;

    public static ContractRegisterResponse from(Long houseId, Long contractId){
        return ContractRegisterResponse.builder()
                .houseId(houseId)
                .contractId(contractId)
                .build();
    }
}
