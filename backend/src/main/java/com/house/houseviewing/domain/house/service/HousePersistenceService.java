package com.house.houseviewing.domain.house.service;

import com.house.houseviewing.domain.common.Address;
import com.house.houseviewing.domain.house.dto.request.HouseEditRequest;
import com.house.houseviewing.domain.house.dto.request.HouseRegisterRequest;
import com.house.houseviewing.domain.house.dto.response.HouseEditResponse;
import com.house.houseviewing.domain.house.dto.response.HouseRegisterResponse;
import com.house.houseviewing.domain.house.entity.HouseEntity;
import com.house.houseviewing.domain.house.repository.HouseRepository;
import com.house.houseviewing.domain.user.entity.UserEntity;
import com.house.houseviewing.domain.user.enums.MonitoringStatus;
import com.house.houseviewing.domain.user.repository.UserRepository;
import com.house.houseviewing.global.exception.AppException;
import com.house.houseviewing.global.exception.ExceptionCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HousePersistenceService {

    private final UserRepository userRepository;
    private final HouseRepository houseRepository;

    @Transactional
    public HouseRegisterResponse register(Long userId, HouseRegisterRequest request, Address address) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ExceptionCode.USER_NOT_FOUND));
        HouseEntity house = request.toEntity(address, MonitoringStatus.OFFLINE);
        if (user.isPremium()) {
            house.updateMonitoringStatus(MonitoringStatus.LIVE);
        }
        user.addHouse(house);
        houseRepository.save(house);

        return HouseRegisterResponse.from(house.getId());
    }

    @Transactional
    public HouseEditResponse editHouse(Long userId, Long houseId, HouseEditRequest request, Address address) {
        HouseEntity house = houseRepository.findByUserIdAndId(userId, houseId)
                .orElseThrow(() -> new AppException(ExceptionCode.HOUSE_NOT_FOUND));
        if (request.getNickname() != null) {
            house.updateNickname(request.getNickname());
        }
        if (address != null) {
            house.updateAddress(address);
        }

        return HouseEditResponse.from(house);
    }
}
