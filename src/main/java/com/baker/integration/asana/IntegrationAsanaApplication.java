package com.baker.integration.asana;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class IntegrationAsanaApplication {

    public static void main(String[] args) {
        SpringApplication.run(IntegrationAsanaApplication.class, args);
    }
}
