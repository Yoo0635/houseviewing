package com.house.houseviewing.fixture;

import com.house.houseviewing.domain.common.Address;

public class AddressFixture {

    public static Address.AddressBuilder createAddress(){
        return Address.builder()
                .addressName("서울 강남구 역삼동 830-31, 105호")
                .region1DepthName("서울")
                .region2DepthName("강남구")
                .region3DepthName("역삼동")
                .mainAddressNo("830")
                .subAddressNo("31")
                .detailAddress("105호");
    }
}
