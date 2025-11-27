package com.phegon.FoodApp.config;

import com.phegon.FoodApp.aws.AWSS3Service;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.web.multipart.MultipartFile;

import java.net.URL;

/**
 * Fake S3 service dùng cho Integration Test.
 * Không gọi AWS thật – luôn trả URL giả.
 */
@TestConfiguration
public class FakeS3Config {

    @Bean
    public AWSS3Service fakeS3Service() {
        return new AWSS3Service() {

            @Override
            public URL uploadFile(String keyName, MultipartFile file) {
                try {
                    // Trả URL giả → tránh gọi AWS thật
                    return new URL("http://localhost/fake/" + keyName);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public void deleteFile(String keyName) {
                // Không làm gì cả – delete giả
            }
        };
    }
}
