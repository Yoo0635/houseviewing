package com.house.houseviewing.domain.contract.controller;

import com.house.houseviewing.domain.contract.entity.ContractEntity;
import com.house.houseviewing.domain.contract.dto.request.ContractRegisterRequest;
import com.house.houseviewing.domain.contract.dto.response.ContractRegisterResponse;
import com.house.houseviewing.domain.contract.service.ContractService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/contracts")
public class ContractController {

    private final ContractService contractService;

    @PostMapping("/register")
    public ResponseEntity<ContractRegisterResponse> join(@Valid @RequestBody ContractRegisterRequest request){
        ContractRegisterResponse result = contractService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @DeleteMapping("/delete/{contractId}")
    public ResponseEntity<Void> deleteContract(@PathVariable Long contractId){
        contractService.delete(contractId);
        return ResponseEntity.noContent().build();
    }
}
