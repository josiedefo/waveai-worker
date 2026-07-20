package com.waveai.worker;

import com.waveai.worker.config.SyncProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(SyncProperties.class)
public class WaveaiWorkerApplication {

    public static void main(String[] args) {
        SpringApplication.run(WaveaiWorkerApplication.class, args);
    }
}
