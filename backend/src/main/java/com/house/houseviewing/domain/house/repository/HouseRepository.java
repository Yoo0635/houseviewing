package com.house.houseviewing.domain.house.repository;

import com.house.houseviewing.domain.house.entity.HouseEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface HouseRepository extends JpaRepository<HouseEntity, Long> {

    List<HouseEntity> findByUserId(Long userId);
    Optional<HouseEntity> findByUserIdAndId(Long userId, Long houseId);
}
