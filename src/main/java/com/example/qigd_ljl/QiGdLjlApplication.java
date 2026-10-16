package com.example.qigd_ljl;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class QiGdLjlApplication {

    public static void main(String[] args) {
        SpringApplication.run(QiGdLjlApplication.class, args);
    }

}