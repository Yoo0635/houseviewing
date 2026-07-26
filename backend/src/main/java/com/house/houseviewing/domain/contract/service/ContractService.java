package com.house.houseviewing.domain.contract.service;

import com.house.houseviewing.domain.contract.dto.response.ContractRegisterResponse;
import com.house.houseviewing.domain.contract.entity.ContractEntity;
import com.house.houseviewing.domain.contract.dto.request.ContractRegisterRequest;
import com.house.houseviewing.domain.contract.repository.ContractRepository;
import com.house.houseviewing.domain.house.entity.HouseEntity;
import com.house.houseviewing.domain.house.repository.HouseRepository;
import com.house.houseviewing.global.exception.AppException;
import com.house.houseviewing.global.exception.ExceptionCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ContractService {

    private final HouseRepository houseRepository;
    private final ContractRepository contractRepository;

    @Transactional
    public ContractRegisterResponse register(ContractRegisterRequest request){
        HouseEntity house = houseRepository.findById(request.getHouseId())
                .orElseThrow(() -> new AppException(ExceptionCode.HOUSE_NOT_FOUND));
        ContractEntity contract = request.toEntity();
        house.addContract(contract);
        ContractEntity savedContract = contractRepository.save(contract);

        return ContractRegisterResponse.from(house.getId(), savedContract.getId());
    }

    @Transactional
    public void delete(Long contractId){
        ContractEntity byId = contractRepository.findById(contractId)
                .orElseThrow(() -> new AppException(ExceptionCode.CONTRACT_NOT_FOUND));
        contractRepository.delete(byId);
    }
}
