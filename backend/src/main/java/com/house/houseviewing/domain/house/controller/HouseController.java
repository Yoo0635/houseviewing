package com.house.houseviewing.domain.house.controller;

import com.house.houseviewing.domain.house.dto.request.HouseEditRequest;
import com.house.houseviewing.domain.house.dto.response.HouseEditResponse;
import com.house.houseviewing.domain.house.dto.response.HouseMeResponse;
import com.house.houseviewing.domain.house.dto.response.HousesResponse;
import com.house.houseviewing.domain.house.dto.request.HouseRegisterRequest;
import com.house.houseviewing.domain.house.dto.response.HouseRegisterResponse;
import com.house.houseviewing.domain.house.service.HouseService;
import com.house.houseviewing.domain.auth.model.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/houses")
public class HouseController {

    private final HouseService houseService;

    @PostMapping("/register")
    public ResponseEntity<HouseRegisterResponse> register(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody HouseRegisterRequest request){
        HouseRegisterResponse result = houseService.register(userDetails.getUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @GetMapping("/{houseId}")
    public ResponseEntity<HouseMeResponse> getHouse(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long houseId){
        HouseMeResponse result = houseService.getHouse(userDetails.getUserId(), houseId);
        return ResponseEntity.ok(result);
    }

    @GetMapping
    public ResponseEntity<List<HousesResponse>> getHouses(@AuthenticationPrincipal CustomUserDetails userDetails){
        List<HousesResponse> house = houseService.getHouses(userDetails.getUserId());
        return ResponseEntity.ok(house);
    }

    @PatchMapping("/{houseId}")
    public ResponseEntity<HouseEditResponse> editHouse(
            @PathVariable Long houseId,
            @Valid @RequestBody HouseEditRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails){
        HouseEditResponse result = houseService.editHouse(userDetails.getUserId(), houseId, request);
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/{houseId}")
    public ResponseEntity<Void> deleteHouse(
            @PathVariable Long houseId,
            @AuthenticationPrincipal CustomUserDetails userDetails){
        houseService.delete(userDetails.getUserId(),houseId);
        return ResponseEntity.noContent().build();
    }
}
