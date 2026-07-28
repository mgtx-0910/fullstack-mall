package com.example.fullstackmall.service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Spring Boot 应用启动入口。
 */
@EnableScheduling
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class FullstackMallApplication {

    public static void main(String[] args) {
        SpringApplication.run(FullstackMallApplication.class, args);
    }
}
