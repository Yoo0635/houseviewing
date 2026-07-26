package com.house.houseviewing.global.external.kakao.service;

import com.house.houseviewing.domain.common.Address;

public interface KakaoAddress {

    Address parsingAddress(String query);
}
