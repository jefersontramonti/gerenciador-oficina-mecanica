package com.pitstop;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class PitStopApplication {

    public static void main(String[] args) {
        SpringApplication.run(PitStopApplication.class, args);
    }

}
