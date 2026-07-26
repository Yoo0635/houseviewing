package com.house.houseviewing.fixture;

import com.house.houseviewing.domain.contract.entity.ContractEntity;
import com.house.houseviewing.domain.contract.enums.ContractType;
import com.house.houseviewing.domain.contract.dto.request.ContractRegisterRequest;
import com.house.houseviewing.domain.house.entity.HouseEntity;

import java.time.LocalDate;

public class ContractFixture {

    public static ContractEntity.ContractEntityBuilder createDefault(HouseEntity house){
        return ContractEntity.builder()
                .contractType(ContractType.JEONSE)
                .deposit(30000000L)
                .monthlyAmount(0L)
                .maintenanceFee(150000L)
                .moveDate(LocalDate.of(2026, 03, 01))
                .confirmDate(LocalDate.of(2026, 03, 07));
    }

    public static ContractEntity createWithHouseAndId(HouseEntity house, Long id){
        ContractEntity entity = ContractEntity.builder()
                .contractType(ContractType.JEONSE)
                .deposit(30000000L)
                .monthlyAmount(0L)
                .maintenanceFee(150000L)
                .moveDate(LocalDate.of(2026, 03, 01))
                .confirmDate(LocalDate.of(2026, 03, 07))
                .build();
        entity.addHouse(house);
        try {
            java.lang.reflect.Field field = ContractEntity.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return entity;
    }

    public static ContractRegisterRequest.ContractRegisterRequestBuilder createRegister(ContractEntity contract){
        return ContractRegisterRequest.builder()
                .houseId(1L)
                .contractType(contract.getContractType())
                .deposit(contract.getDeposit())
                .monthlyAmount(contract.getMonthlyAmount())
                .maintenanceFee(contract.getMaintenanceFee())
                .moveDate(contract.getMoveDate())
                .confirmDate(contract.getConfirmDate());
    }
}
