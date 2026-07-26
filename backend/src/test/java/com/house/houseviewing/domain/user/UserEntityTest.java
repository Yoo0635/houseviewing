package com.house.houseviewing.domain.user;

import com.house.houseviewing.domain.house.entity.HouseEntity;
import com.house.houseviewing.domain.subscription.entity.SubscriptionEntity;
import com.house.houseviewing.domain.subscription.enums.PlanType;
import com.house.houseviewing.domain.user.entity.UserEntity;
import com.house.houseviewing.fixture.HouseFixture;
import com.house.houseviewing.fixture.SubscriptionFixture;
import com.house.houseviewing.fixture.UserFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class UserEntityTest {

    @Test
    @DisplayName("집 추가")
    void 집_추가(){
        // given
        UserEntity user = UserFixture.createDefault().build();
        HouseEntity house = HouseFixture.createDefault(user).build();

        // when
        user.addHouse(house);

        // then
        assertThat(user.getHouses()).contains(house);
        assertThat(user.getHouses()).hasSize(1);
        assertThat(house.getUser()).isEqualTo(user);
    }

    @Test
    @DisplayName("구독권 FREE 체크")
    void 구독권_FREE_체크(){
        // given
        UserEntity user = UserFixture.createDefault().build();
        SubscriptionEntity subscription = SubscriptionFixture.createDefault(user).build();
        // when
        user.updateSubscription(subscription);
        // then
        assertThat(user.getSubscription().getPlanType()).isEqualTo(PlanType.FREE);
        assertThat(user.isPremium()).isFalse();
    }
}
