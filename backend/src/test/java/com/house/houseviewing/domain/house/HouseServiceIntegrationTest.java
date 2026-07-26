package com.house.houseviewing.domain.house;

import com.house.houseviewing.domain.house.entity.HouseEntity;
import com.house.houseviewing.domain.house.dto.request.HouseRegisterRequest;
import com.house.houseviewing.domain.house.repository.HouseRepository;
import com.house.houseviewing.domain.house.service.HouseService;
import com.house.houseviewing.domain.user.entity.UserEntity;
import com.house.houseviewing.domain.user.dto.request.UserRegisterRequest;
import com.house.houseviewing.domain.user.repository.UserRepository;
import com.house.houseviewing.domain.user.service.UserService;
import com.house.houseviewing.fixture.AddressFixture;
import com.house.houseviewing.fixture.HouseFixture;
import com.house.houseviewing.fixture.UserFixture;
import com.house.houseviewing.global.external.kakao.service.KakaoAddress;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.*;
import static org.mockito.ArgumentMatchers.anyString;

@SpringBootTest
@Transactional
public class HouseServiceIntegrationTest {

    @Autowired UserService userService;
    @Autowired UserRepository userRepository;
    @Autowired HouseRepository houseRepository;
    @Autowired HouseService houseService;

    @MockitoBean KakaoAddress kakaoAddress;

    @Test
    @DisplayName("집 등록")
    void 집_등록(){
        UserEntity user = getUserEntity();
        HouseEntity house = getHouseEntity(user);
        assertThat(house.getNickname()).isEqualTo("자취방");
    }

    @Test
    @DisplayName("집 삭제")
    void 집_삭제(){
        UserEntity user = getUserEntity();
        HouseEntity house = getHouseEntity(user);
        houseService.delete(user.getId(), house.getId());
        assertThat(houseRepository.findById(house.getId())).isEmpty();
    }

    private HouseEntity getHouseEntity(UserEntity user) {
        given(kakaoAddress.parsingAddress(anyString())).willReturn(AddressFixture.createAddress().build());
        HouseEntity house = HouseFixture.createDefault(user).build();
        HouseRegisterRequest request = HouseFixture.createRegister(house).build();
        houseService.register(user.getId(), request);
        return houseRepository.findByUserId(user.getId()).stream().findFirst().orElseThrow();
    }

    private UserEntity getUserEntity() {
        UserEntity build = UserFixture.createDefault().build();
        UserRegisterRequest build1 = UserFixture.createRegister(build).build();
        userService.register(build1);
        return userRepository.findByLoginId(build.getLoginId()).orElseThrow();
    }
}
