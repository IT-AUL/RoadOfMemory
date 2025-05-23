package com.itaul.rofm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class RoadOfMemoryApplication {

    public static void main(String[] args) {
        SpringApplication.run(RoadOfMemoryApplication.class, args);
    }
}
