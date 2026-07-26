package com.house.houseviewing.domain.house.dto.response;

import com.house.houseviewing.domain.house.entity.HouseEntity;
import com.house.houseviewing.domain.analysis.postanalysis.entity.PostAnalysisEntity;
import com.house.houseviewing.domain.user.enums.MonitoringStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor @Builder
public class HousesResponse {

    private Long houseId;

    private String nickname;

    private String address;

    private Integer ltvScore;

    private MonitoringStatus monitoringStatus;

    public static HousesResponse from(HouseEntity house){

        Integer ltvScore = null;

        if (!house.getAnalyses().isEmpty()){
            PostAnalysisEntity analysis = house.getAnalyses()
                    .get(house.getAnalyses().size() - 1);
            ltvScore = analysis.getLtvScore();
        }
        return HousesResponse.builder()
                .houseId(house.getId())
                .nickname(house.getNickname())
                .address(house.getAddress().getAddressName())
                .ltvScore(ltvScore)
                .monitoringStatus(house.getMonitoringStatus())
                .build();
    }
}
