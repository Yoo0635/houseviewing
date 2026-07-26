package com.house.houseviewing.domain.auth.dto.response;

import com.house.houseviewing.domain.auth.model.CustomUserDetails;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@AllArgsConstructor
@Getter @Builder
public class LoginResponse {

    private String accessToken;

    private String refreshToken;

    private Long userId;

    private String loginId;

    private String name;

    public static LoginResponse from(CustomUserDetails user, String accessToken, String refreshToken){
        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .userId(user.getUserId())
                .loginId(user.getUsername())
                .name(user.getName())
                .build();
    }
}
