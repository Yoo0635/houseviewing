package com.house.houseviewing.domain.contract;

import com.house.houseviewing.domain.contract.entity.ContractEntity;
import com.house.houseviewing.domain.contract.dto.request.ContractRegisterRequest;
import com.house.houseviewing.domain.contract.repository.ContractRepository;
import com.house.houseviewing.domain.contract.service.ContractService;
import com.house.houseviewing.domain.house.entity.HouseEntity;
import com.house.houseviewing.domain.house.dto.request.HouseRegisterRequest;
import com.house.houseviewing.domain.house.repository.HouseRepository;
import com.house.houseviewing.domain.house.service.HouseService;
import com.house.houseviewing.domain.user.entity.UserEntity;
import com.house.houseviewing.domain.user.dto.request.UserRegisterRequest;
import com.house.houseviewing.domain.user.repository.UserRepository;
import com.house.houseviewing.domain.user.service.UserService;
import com.house.houseviewing.fixture.AddressFixture;
import com.house.houseviewing.fixture.ContractFixture;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.*;

@SpringBootTest
@Transactional
public class ContractServiceIntegrationTest {

    @Autowired ContractService contractService;
    @Autowired ContractRepository contractRepository;
    @Autowired HouseService houseService;
    @Autowired UserService userService;
    @Autowired UserRepository userRepository;
    @Autowired HouseRepository houseRepository;

    @MockitoBean KakaoAddress kakaoAddress;

    @Test
    @DisplayName("계약 등록")
    void 계약_등록(){
        UserEntity user = getUserEntity();
        HouseEntity house = getHouseEntity(user);
        ContractEntity contract = getContract(house);
        assertThat(contract.getHouse().getId()).isEqualTo(house.getId());
    }

    @Test
    @DisplayName("계약 삭제")
    void 계약_삭제(){
        UserEntity user = getUserEntity();
        HouseEntity house = getHouseEntity(user);
        ContractEntity contract = getContract(house);
        contractService.delete(contract.getId());
        assertThat(contractRepository.findById(contract.getId())).isEmpty();
    }

    private ContractEntity getContract(HouseEntity house) {
        ContractEntity contract = ContractFixture.createDefault(house).build();
        ContractRegisterRequest request = ContractFixture.createRegister(contract)
                .houseId(house.getId()).build();
        contractService.register(request);
        return contractRepository.findTopByHouseIdOrderByCreatedAtDesc(house.getId()).orElseThrow();
    }

    private HouseEntity getHouseEntity(UserEntity user) {
        given(kakaoAddress.parsingAddress(anyString())).willReturn(AddressFixture.createAddress().build());
        HouseEntity house = HouseFixture.createDefault(user).build();
        HouseRegisterRequest request = HouseFixture.createRegister(house).build();
        houseService.register(user.getId(), request);
        return houseRepository.findByUserId(user.getId()).stream().findFirst().orElseThrow();
    }

    private UserEntity getUserEntity() {
        UserEntity user = UserFixture.createDefault().build();
        UserRegisterRequest requestUser = UserFixture.createRegister(user).build();
        userService.register(requestUser);
        return userRepository.findByLoginId(user.getLoginId()).orElseThrow();
    }
}
