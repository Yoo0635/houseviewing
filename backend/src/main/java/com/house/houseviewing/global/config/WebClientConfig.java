package com.house.houseviewing.global.config;

import io.netty.channel.ChannelOption;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.codec.ClientCodecConfigurer;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

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
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 2000)
                .responseTimeout(Duration.ofSeconds(3));
        return WebClient.builder()
                .baseUrl(kakaoUrl)
                .defaultHeader("Authorization", "KakaoAK " + kakaoKey )
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }
}
