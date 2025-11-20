package com.phegon.FoodApp.integration;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import software.amazon.awssdk.services.s3.S3Client;

import static org.mockito.Mockito.mock;

@TestConfiguration
public class FakeS3Config {

    @Bean
    @Primary
    public S3Client fakeS3Client() {
        return mock(S3Client.class);
    }
}

