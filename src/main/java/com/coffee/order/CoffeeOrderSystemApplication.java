package com.coffee.order;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;

@SpringBootApplication
@EntityScan("com.coffee.order.domain")
public class CoffeeOrderSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(CoffeeOrderSystemApplication.class, args);
    }
}