package com.house.houseviewing.domain.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Getter @Builder
public class UserResetPasswordRequest {

    @NotBlank(message = "refresh token이 없습니다.")
    private String refreshToken;

    @Size(min = 8, message = "비밀번호를 8자 이상 입력해주세요.")
    @NotBlank(message = "새 비밀번호를 입력해주세요.")
    private String newPassword;

    @NotBlank(message = "비밀번호 확인을 입력해주세요.")
    private String confirmPassword;
}
