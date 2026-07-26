package com.house.houseviewing.fixture;

import com.house.houseviewing.domain.subscription.entity.SubscriptionEntity;
import com.house.houseviewing.domain.subscription.enums.PlanType;
import com.house.houseviewing.domain.user.entity.UserEntity;
import com.house.houseviewing.domain.user.dto.request.UserFindIdRequest;
import com.house.houseviewing.domain.user.dto.request.UserVerifyPasswordRequest;
import com.house.houseviewing.domain.user.dto.request.UserRegisterRequest;

public class UserFixture {

    public static UserEntity createDefaultBuilt(){
        UserEntity entity = UserEntity.builder()
                .name("유인근")
                .loginId("yooyoo9191")
                .password("okok0630!")
                .email("yooyoo9191@gmail.com")
                .build();
        entity.addSubscription(SubscriptionEntity.builder()
                .planType(PlanType.FREE)
                .build());
        return entity;
    }

    public static UserEntity.UserEntityBuilder createDefault(){
        return UserEntity.builder()
                .name("유인근")
                .loginId("yooyoo9191")
                .password("okok0630!")
                .email("yooyoo9191@gmail.com");
    }

    public static UserEntity createPremium(){
        UserEntity entity = UserEntity.builder()
                .name("유인근")
                .loginId("yooyoo9191")
                .password("okok0635!")
                .email("yooyoo9191@gmail.com")
                .build();
        entity.addSubscription(SubscriptionEntity.builder()
                .planType(PlanType.PREMIUM)
                .build());
        return entity;
    }

    public static UserEntity createDefaultWithId(Long id){
        UserEntity entity = UserEntity.builder()
                .name("유인근")
                .loginId("yooyoo9191")
                .password("okok0635!")
                .email("yooyoo9191@gmail.com")
                .build();
        entity.addSubscription(SubscriptionEntity.builder()
                .planType(PlanType.FREE)
                .build());
        try {
            java.lang.reflect.Field field = UserEntity.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return entity;
    }

    public static UserRegisterRequest.UserRegisterRequestBuilder createRegister(UserEntity user){
        return UserRegisterRequest.builder()
                .name(user.getName())
                .email(user.getEmail())
                .loginId(user.getLoginId())
                .password(user.getPassword());
    }

    public static UserFindIdRequest.UserFindIdRequestBuilder createFindId(UserEntity user){
        return UserFindIdRequest.builder()
                .name(user.getName())
                .email(user.getEmail());
    }

    public static UserVerifyPasswordRequest.UserVerifyPasswordRequestBuilder createVerifyPassword(UserEntity user){
        return UserVerifyPasswordRequest.builder()
                .name(user.getName())
                .loginId(user.getLoginId())
                .email(user.getEmail());
    }
}
