package com.house.houseviewing.domain.user.dto.response;

import com.house.houseviewing.domain.common.Address;
import com.house.houseviewing.domain.house.entity.HouseEntity;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UserHousesResponse {

    private Long id;
    private Address address;
    private Long deposit;

    public UserHousesResponse(HouseEntity house) {
        this.id = house.getId();
        this.address = house.getAddress();

        if (house.getContracts() != null && !house.getContracts().isEmpty()){
            this.deposit = house.getContracts().get(0).getDeposit();
        }
        else {
            this.deposit = 0L;
        }
    }
}
