package com.house.houseviewing.fixture;

import com.house.houseviewing.domain.analysis.preanalysis.dto.request.PreContractDiagnosisRequest;
import com.house.houseviewing.domain.analysis.preanalysis.entity.PreAnalysisEntity;
import com.house.houseviewing.domain.common.RiskLevel;
import com.house.houseviewing.domain.user.entity.UserEntity;

public class PreAnalysisFixture {

    public static PreAnalysisEntity.PreAnalysisEntityBuilder createDefault(UserEntity user){
        return PreAnalysisEntity.builder()
                .nickname("테스트분석")
                .rawData("{\"test\": \"data\"}")
                .mainReason("안전")
                .address(AddressFixture.createAddress().build())
                .riskLevel(RiskLevel.SAFE)
                .ltvScore(80);
    }

    public static PreAnalysisEntity createDefaultWithUser(UserEntity user){
        PreAnalysisEntity entity = PreAnalysisEntity.builder()
                .nickname("테스트분석")
                .rawData("{\"test\": \"data\"}")
                .mainReason("안전")
                .address(AddressFixture.createAddress().build())
                .riskLevel(RiskLevel.SAFE)
                .ltvScore(80)
                .build();
        return entity;
    }

    public static PreAnalysisEntity createWithId(UserEntity user, Long id){
        PreAnalysisEntity entity = createDefaultWithUser(user);
        try {
            java.lang.reflect.Field field = PreAnalysisEntity.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return entity;
    }

    public static PreContractDiagnosisRequest.PreContractDiagnosisRequestBuilder createRequest(){
        return PreContractDiagnosisRequest.builder()
                .nickname("테스트분석")
                .address("서울 강남구 역삼동 830-31");
    }
}
