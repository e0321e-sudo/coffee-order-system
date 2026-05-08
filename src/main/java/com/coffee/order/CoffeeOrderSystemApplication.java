package com.coffee.order;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling  // 스케줄러 기능 활성화
@EntityScan("com.coffee.order.domain")
public class CoffeeOrderSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(CoffeeOrderSystemApplication.class, args);
    }
}