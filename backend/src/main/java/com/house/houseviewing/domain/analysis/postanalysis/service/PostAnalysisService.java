package com.house.houseviewing.domain.analysis.postanalysis.service;

import com.house.houseviewing.domain.analysis.postanalysis.dto.response.AnalysisResponse;
import com.house.houseviewing.domain.analysis.postanalysis.enums.AnalysisType;
import com.house.houseviewing.domain.contract.entity.ContractEntity;
import com.house.houseviewing.domain.contract.repository.ContractRepository;
import com.house.houseviewing.domain.analysis.postanalysis.entity.PostAnalysisEntity;
import com.house.houseviewing.domain.analysis.postanalysis.repository.PostAnalysisRepository;
import com.house.houseviewing.domain.house.entity.HouseEntity;
import com.house.houseviewing.domain.house.repository.HouseRepository;
import com.house.houseviewing.global.exception.AppException;
import com.house.houseviewing.global.exception.ExceptionCode;
import com.house.houseviewing.global.file.diff.service.SnapshotDiffAnalysisService;
import com.house.houseviewing.global.file.snapshot.service.SnapshotAnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostAnalysisService {

    private final ContractRepository contractRepository;
    private final PostAnalysisRepository postAnalysisRepository;
    private final SnapshotAnalysisService snapshotAnalysisService;
    private final SnapshotDiffAnalysisService snapshotDiffAnalysisService;
    private final HouseRepository houseRepository;

    @Transactional
    public PostAnalysisEntity postRegister(Long houseId, MultipartFile snapshot){
        HouseEntity house = houseRepository.findById(houseId)
                .orElseThrow(() -> new AppException(ExceptionCode.HOUSE_NOT_FOUND));
        ContractEntity contract = contractRepository.findTopByHouseIdOrderByCreatedAtDesc(houseId)
                .orElseThrow(() -> new AppException(ExceptionCode.CONTRACT_NOT_FOUND));
        PostAnalysisEntity analysis = snapshotAnalysisService.postAnalyze(snapshot);
        analysis.addHouse(house);
        analysis.addContract(contract);
        return postAnalysisRepository.save(analysis);
    }

    public List<AnalysisResponse> getPostAnalyses(Long userId){
        List<PostAnalysisEntity> postAnalyses = postAnalysisRepository.findAllByUserId(userId);
        return postAnalyses
                .stream()
                .map(AnalysisResponse::from)
                .toList();
    }

    public List<AnalysisResponse> getDiffAnalyses(Long userId){
        List<PostAnalysisEntity> diffAnalyses = postAnalysisRepository.findAllByUserIdAndAnalysisType(userId, AnalysisType.DIFF);
        return diffAnalyses
                .stream()
                .map(AnalysisResponse::from)
                .toList();
    }

    @Transactional
    public PostAnalysisEntity diffRegister(Long houseId, String snapshot){
        HouseEntity house = houseRepository.findById(houseId)
                .orElseThrow(() -> new AppException(ExceptionCode.HOUSE_NOT_FOUND));
        ContractEntity contract = contractRepository.findTopByHouseIdOrderByCreatedAtDesc(houseId)
                .orElseThrow(() -> new AppException(ExceptionCode.CONTRACT_NOT_FOUND));
        PostAnalysisEntity analysis = snapshotDiffAnalysisService.diffAnalyze(snapshot);
        analysis.addHouse(house);
        analysis.addContract(contract);

        return postAnalysisRepository.save(analysis);
    }
}
