package com.house.houseviewing.global.external.kakao.service;

import com.house.houseviewing.domain.common.Address;
import com.house.houseviewing.global.exception.AppException;
import com.house.houseviewing.global.exception.ExceptionCode;
import com.house.houseviewing.global.external.kakao.model.KakaoAddressRS;
import com.house.houseviewing.global.external.kakao.model.KakaoAddressRS.Document;
import com.house.houseviewing.global.external.kakao.model.KakaoAddressRS.ParsedAddress;
import com.house.houseviewing.global.util.AddressUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class KakaoAddressService implements KakaoAddress {

    private final WebClient kakaoWebClient;

    public KakaoAddressService(@Qualifier("kakaoWebClient") WebClient kakaoWebClient) {
        this.kakaoWebClient = kakaoWebClient;
    }

    @Override
    public Address parsingAddress(String query) {
        KakaoAddressRS kakaoAddressRSMono = kakaoWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v2/local/search/address.json")
                        .queryParam("query", query)
                        .build())
                .retrieve()
                .bodyToMono(KakaoAddressRS.class).block();
        Document document = kakaoAddressRSMono.getDocuments().stream()
                .findFirst()
                .orElseThrow(() -> new AppException(ExceptionCode.ADDRESS_NOT_FOUND));

        ParsedAddress parsedAddress = document.getParsedAddress();

        if (parsedAddress == null) {
            throw new AppException(ExceptionCode.ADDRESS_NOT_FOUND);
        }

        return new Address(parsedAddress.getAddressName(), parsedAddress.getRegion1DepthName(),
                parsedAddress.getRegion2DepthName(), parsedAddress.getRegion3DepthName(),
                parsedAddress.getMainAddressNo(), parsedAddress.getSubAddressNo(),
                AddressUtils.extractDetailAddress(query));
    }
}
