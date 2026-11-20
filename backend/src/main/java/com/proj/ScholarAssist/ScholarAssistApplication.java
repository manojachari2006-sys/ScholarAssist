package com.proj.ScholarAssist;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ScholarAssistApplication {
    public static void main(String[] args) {
        SpringApplication.run(ScholarAssistApplication.class, args);
    }

}
