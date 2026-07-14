package com.waveai.worker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class WaveaiWorkerApplication {

    public static void main(String[] args) {
        SpringApplication.run(WaveaiWorkerApplication.class, args);
    }
}
