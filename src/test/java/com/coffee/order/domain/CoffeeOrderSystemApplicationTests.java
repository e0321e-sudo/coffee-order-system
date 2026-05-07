package com.coffee.order.domain;

import com.coffee.order.CoffeeOrderSystemApplication;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = CoffeeOrderSystemApplication.class)
@ActiveProfiles("test")
class CoffeeOrderSystemApplicationTests {

    @Test
    void contextLoads() {
    }
}