package com.house.houseviewing.fixture;

import com.house.houseviewing.domain.analysis.postanalysis.entity.PostAnalysisEntity;
import com.house.houseviewing.domain.analysis.postanalysis.enums.AnalysisType;
import com.house.houseviewing.domain.common.RiskLevel;
import com.house.houseviewing.domain.house.entity.HouseEntity;

public class PostAnalysisFixture {

    public static PostAnalysisEntity.PostAnalysisEntityBuilder createDefault(HouseEntity house){
        return PostAnalysisEntity.builder()
                .riskLevel(RiskLevel.SAFE)
                .analysisType(AnalysisType.BASIC)
                .mainReason("안전")
                .ltvScore(85)
                .rawData("{\"test\": \"data\"}");
    }

    public static PostAnalysisEntity createDefaultWithHouse(HouseEntity house){
        PostAnalysisEntity entity = PostAnalysisEntity.builder()
                .riskLevel(RiskLevel.SAFE)
                .analysisType(AnalysisType.BASIC)
                .mainReason("안전")
                .ltvScore(85)
                .rawData("{\"test\": \"data\"}")
                .build();
        entity.addHouse(house);
        return entity;
    }

    public static PostAnalysisEntity.PostAnalysisEntityBuilder createDiff(HouseEntity house){
        return PostAnalysisEntity.builder()
                .riskLevel(RiskLevel.WARNING)
                .analysisType(AnalysisType.DIFF)
                .mainReason("주의")
                .ltvScore(70)
                .rawData("{\"test\": \"diff\"}");
    }

    public static PostAnalysisEntity createDiffWithHouse(HouseEntity house){
        PostAnalysisEntity entity = PostAnalysisEntity.builder()
                .riskLevel(RiskLevel.WARNING)
                .analysisType(AnalysisType.DIFF)
                .mainReason("주의")
                .ltvScore(70)
                .rawData("{\"test\": \"diff\"}")
                .build();
        entity.addHouse(house);
        return entity;
    }

    public static PostAnalysisEntity createWithId(HouseEntity house, Long id){
        PostAnalysisEntity entity = createDefaultWithHouse(house);
        try {
            java.lang.reflect.Field field = PostAnalysisEntity.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return entity;
    }

    public static PostAnalysisEntity createDiffWithId(HouseEntity house, Long id){
        PostAnalysisEntity entity = createDiffWithHouse(house);
        try {
            java.lang.reflect.Field field = PostAnalysisEntity.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return entity;
    }
}
