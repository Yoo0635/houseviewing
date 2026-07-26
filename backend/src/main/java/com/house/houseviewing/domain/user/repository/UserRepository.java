package com.house.houseviewing.domain.user.repository;

import com.house.houseviewing.domain.user.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<UserEntity, Long> {
    Optional<UserEntity> findByEmailAndName(String email, String name);
    Optional<UserEntity> findByLoginIdAndPassword(String loginId, String password);
    Optional<UserEntity> findByEmailAndNameAndLoginId(String email, String name, String loginId);
    boolean existsByLoginId(String loginId);
    boolean existsByEmail(String email);
    Optional<UserEntity> findByLoginId(String loginId);
}
