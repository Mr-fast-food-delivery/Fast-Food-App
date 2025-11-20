package com.phegon.FoodApp.integration;

import com.phegon.FoodApp.aws.AWSS3Service;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.services.s3.S3Client;

import java.net.URL;

@TestConfiguration
public class FakeS3Config {

    /** 
     * ✔ OVERRIDE S3Client để Spring không load AWS thật 
     */
    @Bean
    @Primary
    public S3Client fakeS3Client() {
        return S3Client.builder().build();  // fake, không dùng thật
    }

    /**
     * ✔ Fake AWSS3Service đúng signature 
     */
    @Bean
    @Primary
    public AWSS3Service awsS3Service() {
        return new AWSS3Service() {

            @Override
            public URL uploadFile(String keyName, MultipartFile file) {
                try {
                    return new URL("https://fake-s3.com/" + keyName);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public void deleteFile(String keyName) {
                // nothing
            }
        };
    }
}
