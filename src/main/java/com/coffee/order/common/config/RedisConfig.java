package com.coffee.order.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // key는 문자열로 저장
        // 예: "popular:menus" 이런 형태로 Redis에 저장됨
        template.setKeySerializer(new StringRedisSerializer());

        // value는 JSON으로 저장
        // 예: Menu 객체 -> {"id":1, "name":"아메리카노", "price":4500} 형태로 저장
        // 나중에 꺼낼 때 다시 Menu 객체로 자동 복원
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());

        return template;
    }
}
