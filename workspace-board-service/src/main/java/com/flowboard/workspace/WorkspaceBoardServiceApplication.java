package com.flowboard.workspace;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class WorkspaceBoardServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(WorkspaceBoardServiceApplication.class, args);
    }
}