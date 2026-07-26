package com.house.houseviewing.domain.user.dto.request;

import com.house.houseviewing.domain.user.entity.UserEntity;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor @Builder
public class UserRegisterRequest {

    @NotBlank(message = "이름은 필수입니다.")
    private String name;

    @NotBlank(message = "이메일은 필수입니다.")
    @Email(message = "이메일 형식이 아닙니다.")
    private String email;

    @NotBlank(message = "아이디는 필수입니다.")
    private String loginId;

    @NotBlank(message = "비밀번호는 필수입니다.")
    @Size(min = 8, message = "비밀번호는 8자리 이상으로 입력해주세요.")
    private String password;

    public UserEntity toEntity(String encodePassword){
        return UserEntity.builder()
                .name(name)
                .email(email)
                .loginId(loginId)
                .password(encodePassword)
                .build();
    }
}
