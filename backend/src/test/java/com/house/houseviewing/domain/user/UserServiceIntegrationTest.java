package com.house.houseviewing.domain.user;

import com.house.houseviewing.domain.user.entity.UserEntity;
import com.house.houseviewing.domain.user.dto.request.UserFindIdRequest;
import com.house.houseviewing.domain.user.dto.request.UserResetPasswordRequest;
import com.house.houseviewing.domain.user.dto.request.UserVerifyPasswordRequest;
import com.house.houseviewing.domain.user.dto.request.UserRegisterRequest;
import com.house.houseviewing.domain.user.repository.UserRepository;
import com.house.houseviewing.domain.user.service.UserService;
import com.house.houseviewing.fixture.UserFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
public class UserServiceIntegrationTest {

    @Autowired UserService userService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("회원가입")
    void 회원가입(){
        UserEntity user = UserFixture.createDefault().build();
        UserRegisterRequest request = UserFixture.createRegister(user).build();
        userService.register(request);
        assertThat(userRepository.existsByLoginId(user.getLoginId())).isTrue();
    }

    @Test
    @DisplayName("아이디 찾기")
    void 아이디_찾기(){
        UserEntity user = getUserEntity();
        UserFindIdRequest findIdRQ = UserFindIdRequest.builder()
                .email(user.getEmail())
                .name(user.getName())
                .build();
        var response = userService.findLoginId(findIdRQ);
        assertThat(user.getLoginId()).isEqualTo(response.getLoginId());
    }

    @Test
    @DisplayName("비밀번호 권한 얻기")
    void 비밀번호_권한_얻기(){
        UserEntity user = getUserEntity();
        UserVerifyPasswordRequest request = UserVerifyPasswordRequest.builder()
                .email(user.getEmail())
                .loginId(user.getLoginId())
                .name(user.getName())
                .build();
        String result = userService.passwordVerify(request);
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("비밀번호 재설정")
    void 비밀번호_재설정(){
        UserEntity user = getUserEntity();
        String resetToken = userService.passwordVerify(UserVerifyPasswordRequest.builder()
                .email(user.getEmail())
                .loginId(user.getLoginId())
                .name(user.getName())
                .build());

        userService.passwordReset(UserResetPasswordRequest.builder()
                .refreshToken(resetToken)
                .newPassword("new-password123")
                .confirmPassword("new-password123")
                .build());

        UserEntity updated = userRepository.findByLoginId(user.getLoginId()).orElseThrow();
        assertThat(passwordEncoder.matches("new-password123", updated.getPassword())).isTrue();
    }

    @Test
    @DisplayName("탈퇴")
    void 탈퇴(){
        UserEntity user = getUserEntity();
        userService.delete(user.getId());
        assertThat(userRepository.findById(user.getId())).isEmpty();
    }

    private UserEntity getUserEntity() {
        UserEntity build = UserFixture.createDefault().build();
        UserRegisterRequest build1 = UserFixture.createRegister(build).build();
        userService.register(build1);
        return userRepository.findByLoginId(build.getLoginId()).orElseThrow();
    }
}
