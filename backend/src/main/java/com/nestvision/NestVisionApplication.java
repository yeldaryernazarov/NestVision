package com.nestvision;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class NestVisionApplication {

    public static void main(String[] args) {
        // Используем docker профиль, если указан в переменных окружения
        String activeProfile = System.getenv("SPRING_PROFILES_ACTIVE");
        if (activeProfile == null || activeProfile.isEmpty()) {
            // Проверяем, запущены ли мы в Docker
            if (System.getenv("SPRING_DATASOURCE_URL") != null) {
                System.setProperty("spring.profiles.active", "docker");
            }
        }
        SpringApplication.run(NestVisionApplication.class, args);
    }
}

