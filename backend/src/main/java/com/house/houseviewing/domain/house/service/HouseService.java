package com.house.houseviewing.domain.house.service;

import com.house.houseviewing.domain.common.Address;
import com.house.houseviewing.domain.contract.entity.ContractEntity;
import com.house.houseviewing.domain.contract.repository.ContractRepository;
import com.house.houseviewing.domain.house.dto.request.HouseEditRequest;
import com.house.houseviewing.domain.house.dto.response.HouseEditResponse;
import com.house.houseviewing.domain.house.dto.response.HouseMeResponse;
import com.house.houseviewing.domain.house.dto.response.HouseRegisterResponse;
import com.house.houseviewing.domain.house.dto.response.HousesResponse;
import com.house.houseviewing.domain.house.entity.HouseEntity;
import com.house.houseviewing.domain.analysis.postanalysis.entity.PostAnalysisEntity;
import com.house.houseviewing.domain.analysis.postanalysis.repository.PostAnalysisRepository;
import com.house.houseviewing.domain.house.dto.request.HouseRegisterRequest;
import com.house.houseviewing.domain.house.repository.HouseRepository;
import com.house.houseviewing.global.exception.AppException;
import com.house.houseviewing.global.exception.ExceptionCode;
import com.house.houseviewing.global.external.kakao.service.KakaoAddress;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HouseService {

    private final HouseRepository houseRepository;
    private final KakaoAddress kakaoAddress;
    private final HousePersistenceService housePersistenceService;
    private final ContractRepository contractRepository;
    private final PostAnalysisRepository postAnalysisRepository;

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public HouseRegisterResponse register(Long userId, HouseRegisterRequest request){
        Address address = kakaoAddress.parsingAddress(request.getOriginAddress());
        return housePersistenceService.register(userId, request, address);
    }

    public HouseMeResponse getHouse(Long userId, Long houseId){
        HouseEntity house = houseRepository.findByUserIdAndId(userId, houseId)
                .orElseThrow(() -> new AppException(ExceptionCode.HOUSE_NOT_FOUND));
        ContractEntity contract = contractRepository.findTopByHouseIdOrderByCreatedAtDesc(house.getId())
                .orElseThrow(() -> new AppException(ExceptionCode.CONTRACT_NOT_FOUND));
        PostAnalysisEntity analysis = postAnalysisRepository.findTopByHouseIdOrderByCreatedAtDesc(house.getId())
                .orElseThrow(() -> new AppException(ExceptionCode.ANALYSIS_NOT_FOUND));
        return HouseMeResponse.from(contract, analysis);
    }

    @Transactional
    public void delete(Long userId, Long houseId){
        HouseEntity saved = houseRepository.findById(houseId)
                .orElseThrow(() -> new AppException(ExceptionCode.HOUSE_NOT_FOUND));

        houseRepository.delete(saved);
    }

    public List<HousesResponse> getHouses(Long userId){
        List<HouseEntity> houses = houseRepository.findByUserId(userId);

        return houses.stream()
                .map(HousesResponse::from)
                .toList();
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public HouseEditResponse editHouse(Long userId, Long houseId, HouseEditRequest request){
        Address address = request.getAddress() == null ? null : kakaoAddress.parsingAddress(request.getAddress());
        return housePersistenceService.editHouse(userId, houseId, request, address);
    }
}
