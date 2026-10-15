package com.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling // Para el Circuit Breaker (@Scheduled)
public class RabbitMQAdvancedApplication {

    public static void main(String[] args) {
        SpringApplication.run(RabbitMQAdvancedApplication.class, args);
    }
}
