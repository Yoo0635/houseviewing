package com.house.houseviewing.global.performance;

import com.house.houseviewing.domain.analysis.postanalysis.entity.PostAnalysisEntity;
import com.house.houseviewing.domain.analysis.postanalysis.enums.AnalysisType;
import com.house.houseviewing.domain.analysis.postanalysis.repository.PostAnalysisRepository;
import com.house.houseviewing.domain.common.RiskLevel;
import com.house.houseviewing.domain.contract.entity.ContractEntity;
import com.house.houseviewing.domain.contract.repository.ContractRepository;
import com.house.houseviewing.domain.house.entity.HouseEntity;
import com.house.houseviewing.domain.house.repository.HouseRepository;
import com.house.houseviewing.global.exception.AppException;
import com.house.houseviewing.global.exception.ExceptionCode;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Profile("performance")
@Service
@RequiredArgsConstructor
public class PerformanceAnalysisSeedService {

    private final HouseRepository houseRepository;
    private final ContractRepository contractRepository;
    private final PostAnalysisRepository postAnalysisRepository;

    @Transactional
    public void seedBaselineAnalysis(Long houseId) {
        HouseEntity house = houseRepository.findById(houseId)
                .orElseThrow(() -> new AppException(ExceptionCode.HOUSE_NOT_FOUND));
        ContractEntity contract = contractRepository.findTopByHouseIdOrderByCreatedAtDesc(houseId)
                .orElseThrow(() -> new AppException(ExceptionCode.CONTRACT_NOT_FOUND));

        PostAnalysisEntity analysis = PostAnalysisEntity.builder()
                .riskLevel(RiskLevel.SAFE)
                .analysisType(AnalysisType.BASIC)
                .mainReason("성능 테스트용 이전 등기 분석")
                .ltvScore(80)
                .rawData("{\"risk\":\"safe\",\"source\":\"performance-baseline\"}")
                .build();
        analysis.addHouse(house);
        analysis.addContract(contract);
        postAnalysisRepository.save(analysis);
    }
}
