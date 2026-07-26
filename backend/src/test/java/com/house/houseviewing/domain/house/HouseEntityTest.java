package com.house.houseviewing.domain.house;

import com.house.houseviewing.domain.contract.entity.ContractEntity;
import com.house.houseviewing.domain.house.entity.HouseEntity;
import com.house.houseviewing.domain.user.entity.UserEntity;
import com.house.houseviewing.fixture.ContractFixture;
import com.house.houseviewing.fixture.HouseFixture;
import com.house.houseviewing.fixture.UserFixture;
import com.house.houseviewing.global.exception.AppException;
import com.house.houseviewing.global.exception.ExceptionCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class HouseEntityTest {

    @Test
    @DisplayName("계약 추가")
    void 계약_추가(){
        // given
        UserEntity user = UserFixture.createDefault().build();
        HouseEntity house = HouseFixture.createDefault(user).build();
        ContractEntity contract = ContractFixture.createDefault(house).build();
        // when
        house.addContract(contract);
        // then
        assertThat(house.getContracts()).contains(contract);
        assertThat(house.getContracts()).hasSize(1);
        assertThat(contract.getHouse()).isEqualTo(house);
    }

    @Test
    @DisplayName("빈 계약 체크")
    void 빈_계약_체크(){
        // given
        UserEntity user = UserFixture.createDefault().build();
        HouseEntity house = HouseFixture.createDefault(user).build();
        ContractEntity contract = ContractFixture.createDefault(house).build();
        // when
        house.addContract(contract);
        // then
        assertThatThrownBy(() -> {
            house.addContract(contract);
        }).isInstanceOf(AppException.class)
                .extracting("exceptionCode")
                .isEqualTo(ExceptionCode.ALREADY_REGISTERED_CONTRACT);
    }

}
