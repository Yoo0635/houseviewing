package com.house.houseviewing.domain.user;

import com.house.houseviewing.domain.user.dto.request.UserFindIdRequest;
import com.house.houseviewing.domain.user.dto.request.UserResetPasswordRequest;
import com.house.houseviewing.domain.user.dto.request.UserVerifyPasswordRequest;
import com.house.houseviewing.domain.user.dto.request.UserRegisterRequest;
import com.house.houseviewing.domain.user.dto.response.UserFindIdResponse;
import com.house.houseviewing.fixture.UserFixture;
import com.house.houseviewing.domain.user.service.UserService;
import com.house.houseviewing.domain.user.entity.UserEntity;
import com.house.houseviewing.domain.user.repository.UserRepository;
import com.house.houseviewing.global.exception.AppException;
import com.house.houseviewing.global.exception.ExceptionCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UserServiceTest {

    @InjectMocks UserService userService;
    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock StringRedisTemplate stringRedisTemplate;
    @Mock ValueOperations<String, String> valueOperations;

    @Nested
    @DisplayName("회원가입")
    class Register{

        @Test
        @DisplayName("성공")
        void 성공(){
            UserEntity build = UserFixture.createDefault().build();
            UserRegisterRequest build1 = UserFixture.createRegister(build).build();
            given(userRepository.existsByEmail(anyString()))
                    .willReturn(false);
            given(userRepository.existsByLoginId(anyString()))
                    .willReturn(false);
            given(passwordEncoder.encode(anyString()))
                    .willReturn("encoded");
            given(userRepository.save(any(UserEntity.class)))
                    .willReturn(build);
            given(stringRedisTemplate.opsForValue()).willReturn(valueOperations);

            userService.register(build1);

            then(userRepository).should(times(1)).save(any(UserEntity.class));
        }

        @Test
        @DisplayName("실패: 아이디 중복")
        void 아이디_중복_예외_발생(){
            UserEntity build = UserFixture.createDefault().build();
            UserRegisterRequest request = UserFixture.createRegister(build).build();

            given(userRepository.existsByLoginId(anyString()))
                    .willReturn(true);

            assertThatThrownBy(() -> userService.register(request))
                    .isInstanceOf(AppException.class)
                    .extracting("exceptionCode")
                    .isEqualTo(ExceptionCode.DUPLICATE_LOGIN_ID);
        }

        @Test
        @DisplayName("실패: 이메일 중복")
        void 이메일_중복_예외_발생(){
            UserEntity build = UserFixture.createDefault().build();
            UserRegisterRequest build1 = UserFixture.createRegister(build).build();

            given(userRepository.existsByLoginId(anyString()))
                    .willReturn(false);
            given(userRepository.existsByEmail(anyString()))
                    .willReturn(true);

            assertThatThrownBy(() -> userService.register(build1))
                    .isInstanceOf(AppException.class)
                    .extracting("exceptionCode")
                    .isEqualTo(ExceptionCode.DUPLICATE_EMAIL);
        }
    }

    @Nested
    @DisplayName("아이디 찾기")
    class FindLoginId{

        @Test
        @DisplayName("성공")
        void 성공(){
            UserEntity user = UserFixture.createDefault().build();
            UserFindIdRequest request = UserFixture.createFindId(user).build();
            given(userRepository.findByEmailAndName(anyString(), anyString()))
                    .willReturn(Optional.of(user));

            UserFindIdResponse result = userService.findLoginId(request);

            assertThat(result).isNotNull();
            assertThat(result.getLoginId()).isEqualTo(user.getLoginId());
        }

        @Test
        @DisplayName("실패: 리소스가 일치하지 않음")
        void 아이디_찾기_실패_에외_발생(){
            UserEntity build = UserFixture.createDefault().build();
            UserFindIdRequest build1 = UserFixture.createFindId(build).build();
            given(userRepository.findByEmailAndName(anyString(), anyString()))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> userService.findLoginId(build1))
                    .isInstanceOf(AppException.class)
                    .extracting("exceptionCode")
                    .isEqualTo(ExceptionCode.FIND_LOGIN_ID_FAILED);
        }
    }

    @Nested
    @DisplayName("비밀번호")
    class Password{

        @Test
        @DisplayName("권한 성공")
        void 권한_성공(){
            UserEntity user = UserFixture.createDefault().build();
            UserVerifyPasswordRequest request = UserFixture.createVerifyPassword(user).build();
            given(userRepository.findByEmailAndNameAndLoginId(anyString(), anyString(), anyString()))
                    .willReturn(Optional.of(user));
            given(stringRedisTemplate.opsForValue()).willReturn(valueOperations);

            String result = userService.passwordVerify(request);

            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("실패: 권한 리소스 일치하지 않음")
        void 비밀번호_권한_에외_발생(){
            UserEntity build = UserFixture.createDefault().build();
            UserVerifyPasswordRequest build1 = UserFixture.createVerifyPassword(build).build();
            given(userRepository.findByEmailAndNameAndLoginId(anyString(), anyString(), anyString()))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> userService.passwordVerify(build1))
                    .isInstanceOf(AppException.class)
                    .extracting("exceptionCode")
                    .isEqualTo(ExceptionCode.VERIFY_PASSWORD_FAILED);
        }

        @Test
        @DisplayName("재설정 성공")
        void 재설정_성공(){
            UserEntity user = UserFixture.createDefault().build();
            UserResetPasswordRequest request = UserResetPasswordRequest.builder()
                    .refreshToken("reset-token")
                    .newPassword("new-password123")
                    .confirmPassword("new-password123")
                    .build();

            given(stringRedisTemplate.opsForValue()).willReturn(valueOperations);
            given(valueOperations.get("PW_RESET_ALLOWED:reset-token")).willReturn(user.getLoginId());
            given(userRepository.findByLoginId(user.getLoginId())).willReturn(Optional.of(user));
            given(passwordEncoder.encode("new-password123")).willReturn("encoded-new-password");

            userService.passwordReset(request);

            then(stringRedisTemplate).should().delete("PW_RESET_ALLOWED:reset-token");
            assertThat(user.getPassword()).isEqualTo("encoded-new-password");
        }
    }

    @Nested
    @DisplayName("유저 삭제")
    class Delete{

        @Test
        @DisplayName("성공")
        void 성공(){
            Long userId = 1L;
            UserEntity user = UserFixture.createDefault().build();
            given(userRepository.findById(userId))
                    .willReturn(Optional.of(user));

            userService.delete(userId);

            then(userRepository).should(times(1)).delete(user);
        }

        @Test
        @DisplayName("실퍠: 해당 유저가 없음")
        void 유저_삭제_예외_발생(){
            UserEntity build = UserFixture.createDefault().build();
            given(userRepository.findById(1L))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> userService.delete(1L))
                    .isInstanceOf(AppException.class)
                    .extracting("exceptionCode")
                    .isEqualTo(ExceptionCode.USER_NOT_FOUND);
        }
    }

}
