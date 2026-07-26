package com.house.houseviewing.fixture;

import com.house.houseviewing.domain.auth.dto.request.LoginRequest;
import com.house.houseviewing.domain.auth.dto.request.ReissueRequest;
import com.house.houseviewing.domain.auth.model.CustomUserDetails;
import com.house.houseviewing.domain.user.entity.UserEntity;

public class AuthFixture {

    public static LoginRequest.LoginRequestBuilder createLoginRequest(){
        return LoginRequest.builder()
                .loginId("yooyoo9191")
                .password("okok0635!");
    }

    public static ReissueRequest.ReissueRequestBuilder createReissueRequest(String refreshToken){
        return ReissueRequest.builder()
                .refreshToken(refreshToken);
    }

    public static CustomUserDetails createUserDetails(UserEntity user){
        return new CustomUserDetails(user);
    }
}
