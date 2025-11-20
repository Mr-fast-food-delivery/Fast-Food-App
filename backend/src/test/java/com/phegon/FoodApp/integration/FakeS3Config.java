package com.phegon.FoodApp.integration;

import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import software.amazon.awssdk.services.s3.S3Client;

@TestConfiguration
public class FakeS3Config {

    @Bean
    @Primary
    public S3Client fakeS3Client() {
        return Mockito.mock(S3Client.class);
    }
}
