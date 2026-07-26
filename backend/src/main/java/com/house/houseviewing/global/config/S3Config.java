package com.house.houseviewing.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
@Conditional(S3Config.S3ClientCondition.class)
public class S3Config {

    @Value("${spring.cloud.aws.region.static}")
    private String region;

    @Value("${AWS_ACCESS_KEY:}")
    private String accessKey;

    @Value("${AWS_SECRET_KEY:}")
    private String secretKey;

    @Bean
    public S3Client s3Client(){
        StaticCredentialsProvider credentialsProvider = StaticCredentialsProvider.create(
                AwsBasicCredentials.create(accessKey, secretKey));

        return S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(credentialsProvider)
                .build();
    }

    @Bean
    public S3Presigner s3Presigner(){
        StaticCredentialsProvider credentialsProvider = StaticCredentialsProvider.create(
                AwsBasicCredentials.create(accessKey, secretKey));

        return S3Presigner.builder()
                .region(Region.of(region))
                .credentialsProvider(credentialsProvider)
                .build();
    }

    static class S3ClientCondition implements org.springframework.context.annotation.Condition {
        @Override
        public boolean matches(org.springframework.context.annotation.ConditionContext context, org.springframework.core.type.AnnotatedTypeMetadata metadata) {
            String accessKey = context.getEnvironment().getProperty("AWS_ACCESS_KEY", "");
            String secretKey = context.getEnvironment().getProperty("AWS_SECRET_KEY", "");
            return !accessKey.isBlank() && !secretKey.isBlank();
        }
    }
}
