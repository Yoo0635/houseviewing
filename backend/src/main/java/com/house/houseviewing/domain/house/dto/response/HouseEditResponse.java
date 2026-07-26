package com.house.houseviewing.domain.house.dto.response;

import com.house.houseviewing.domain.house.entity.HouseEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor @Getter
@AllArgsConstructor @Builder
public class HouseEditResponse {

    private String address;

    private String nickname;

    public static HouseEditResponse from(HouseEntity house){
        return HouseEditResponse.builder()
                .address(house.getAddress().getAddressName())
                .nickname(house.getNickname())
                .build();
    }
}
