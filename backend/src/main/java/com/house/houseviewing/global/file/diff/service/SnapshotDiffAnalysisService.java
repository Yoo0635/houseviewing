package com.house.houseviewing.global.file.diff.service;

import com.house.houseviewing.domain.analysis.postanalysis.entity.PostAnalysisEntity;
import com.house.houseviewing.domain.analysis.postanalysis.enums.AnalysisType;
import com.house.houseviewing.global.exception.AppException;
import com.house.houseviewing.global.exception.ExceptionCode;
import com.house.houseviewing.global.file.diff.dto.DiffAnalysisResult;
import com.house.houseviewing.infrastructure.python.PythonEngineClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SnapshotDiffAnalysisService {

    private final PythonEngineClient pythonEngineClient;

    public PostAnalysisEntity diffAnalyze(String snapshot){
        try{
            DiffAnalysisResult result = pythonEngineClient.diffSendSnapshot(snapshot).block();

            if(result == null){
                throw new AppException(ExceptionCode.ANALYSIS_FAILED);
            }

            return PostAnalysisEntity.builder()
                    .analysisType(AnalysisType.DIFF)
                    .mainReason(result.getMainReason())
                    .rawData(result.getRawData())
                    .riskLevel(result.getRiskLevel())
                    .ltvScore(result.getLtvScore())
                    .build();

        } catch (Exception e){
            throw new AppException(ExceptionCode.ANALYSIS_FAILED);
        }
    }
}
