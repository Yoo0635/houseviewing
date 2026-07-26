package com.house.houseviewing.global.external.kakao.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.util.List;

@Getter
@NoArgsConstructor @AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class KakaoAddressRS {

    private List<Document> documents;

    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Document{
        @JsonProperty("address")
        private ParsedAddress parsedAddress;
    }

    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ParsedAddress{
        @JsonProperty("address_name")
        private String addressName; // 경기도 오산시 양산동 387, 105호

        @JsonProperty("region_1depth_name")
        private String region1DepthName; // 경기도, 서울시

        @JsonProperty("region_2depth_name")
        private String region2DepthName; // 오산시, 송파구

        @JsonProperty("region_3depth_name")
        private String region3DepthName; // 양산동, 송파동

        @JsonProperty("main_address_no")
        private String mainAddressNo; // 387, 173-6

        @JsonProperty("sub_address_no")
        private String subAddressNo; // 105호
    }
}
