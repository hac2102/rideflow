package com.rideflow.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class RideFlowServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(RideFlowServerApplication.class, args);
    }
}
