package com.house.houseviewing.domain.house.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor @Builder
public class HouseRegisterResponse {

    private Long houseId;

    public static HouseRegisterResponse from(Long houseId){
        return HouseRegisterResponse.builder()
                .houseId(houseId)
                .build();
    }
}

