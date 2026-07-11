package com.hotchpotch.lottery;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class LotteryBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(LotteryBackendApplication.class, args);
    }

}
