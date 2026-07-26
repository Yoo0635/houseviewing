package com.house.houseviewing.domain.common;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor
@AllArgsConstructor @Builder
public class Address {

    private String addressName; // 서울 강남구 역삼동 830-31, 105호

    private String region1DepthName; // 서울

    private String region2DepthName; // 강남구

    private String region3DepthName; // 역삼동

    private String mainAddressNo; // 830

    private String subAddressNo; // 31

    private String detailAddress; // 105호

}