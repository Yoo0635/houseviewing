package com.house.houseviewing.global.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.codec.ClientCodecConfigurer;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Bean
    @Qualifier("pythonWebClient")
    public WebClient pythonWebClient(
            @Value("${python.api.url}") String pythonUrl){
        ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
                .build();
        return WebClient.builder()
                .baseUrl(pythonUrl)
                .exchangeStrategies(strategies)
                .build();
    }

    @Bean
    @Qualifier("kakaoWebClient")
    public WebClient kakaoWebClient(
            @Value("${kakao.api.key}") String kakaoKey,
            @Value("${kakao.api.url}") String kakaoUrl){
        return WebClient.builder()
                .baseUrl(kakaoUrl)
                .defaultHeader("Authorization", "KakaoAK " + kakaoKey )
                .build();
    }
}
