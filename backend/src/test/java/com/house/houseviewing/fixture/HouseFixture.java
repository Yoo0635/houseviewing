package com.house.houseviewing.fixture;

import com.house.houseviewing.domain.house.entity.HouseEntity;
import com.house.houseviewing.domain.house.dto.request.HouseRegisterRequest;
import com.house.houseviewing.domain.user.entity.UserEntity;
import com.house.houseviewing.domain.user.enums.MonitoringStatus;

public class HouseFixture {

    public static HouseEntity.HouseEntityBuilder createDefault(UserEntity user){
        return HouseEntity.builder()
                .nickname("자취방")
                .monitoringStatus(MonitoringStatus.OFFLINE)
                .address(AddressFixture.createAddress().build());
    }

    public static HouseEntity createWithUserAndId(UserEntity user, Long id){
        HouseEntity entity = HouseEntity.builder()
                .nickname("자취방")
                .monitoringStatus(MonitoringStatus.OFFLINE)
                .address(AddressFixture.createAddress().build())
                .build();
        entity.addUser(user);
        try {
            java.lang.reflect.Field field = HouseEntity.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return entity;
    }

    public static HouseRegisterRequest.HouseRegisterRequestBuilder createRegister(HouseEntity house){
        return HouseRegisterRequest.builder()
                .nickname(house.getNickname())
                .originAddress("서울시 강남구");
    }
}
