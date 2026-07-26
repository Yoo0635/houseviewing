package com.house.houseviewing.domain.contract.repository;

import com.house.houseviewing.domain.contract.entity.ContractEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ContractRepository extends JpaRepository<ContractEntity, Long> {

    Optional<ContractEntity> findTopByHouseIdOrderByCreatedAtDesc(Long houseId);
}
