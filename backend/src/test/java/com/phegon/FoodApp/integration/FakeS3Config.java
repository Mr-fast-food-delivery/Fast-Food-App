package com.phegon.FoodApp.integration;

import com.phegon.FoodApp.aws.AWSS3Service;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.web.multipart.MultipartFile;

import java.net.MalformedURLException;
import java.net.URL;

@TestConfiguration
public class FakeS3Config {

    @Bean
    @Primary
    public AWSS3Service awsS3Service() {
        return new AWSS3Service() {

            @Override
            public URL uploadFile(String keyName, MultipartFile file) {
                try {
                    return new URL("https://fake-s3.com/" + keyName);
                } catch (MalformedURLException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public void deleteFile(String keyName) {
                // Không làm gì — fake thôi
            }
        };
    }
}
