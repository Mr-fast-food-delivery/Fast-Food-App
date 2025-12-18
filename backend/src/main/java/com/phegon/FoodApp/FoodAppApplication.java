package com.phegon.FoodApp;

import com.phegon.FoodApp.role.entity.Role;
import com.phegon.FoodApp.role.repository.RoleRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableAsync
@EnableTransactionManagement 
public class FoodAppApplication {

    public static void main(String[] args) {
        SpringApplication.run(FoodAppApplication.class, args);
    }

    @Bean
    CommandLineRunner initRoles(RoleRepository roleRepository) {
        return args -> {
            if (roleRepository.count() == 0) {
                roleRepository.save(new Role(null, "CUSTOMER"));
                roleRepository.save(new Role(null, "ADMIN"));
                System.out.println("Default roles inserted into database.");
            }
        };
    }
}
