package com.coffee.order.common.config;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

@EnableKafka        // @KafkaListener 어노테이션 활성화
@Configuration
public class KafkaConfig {

    // application.yml의 bootstrap-servers 값 주입
    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;


    // Producer 설정
    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> props = new HashMap<>();
        // Kafka 브로커 주소 (kafka-1, kafka-2, kafka-3)
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        // 메시지 key를 문자열로 직렬화 (토픽 이름 등)
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        // 메시지 value를 JSON으로 직렬화 (OrderCompletedEvent 같은 객체)
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        return new DefaultKafkaProducerFactory<>(props);
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        // Producer가 실제로 메시지를 보낼 때 사용하는 템플릿
        // kafkaTemplate.send("토픽이름", 객체) 형태로 사용
        return new KafkaTemplate<>(producerFactory());
    }

    // Consumer 설정
    @Bean
    public ConsumerFactory<String, Object> consumerFactory() {
        Map<String, Object> props = new HashMap<>();
        // Kafka 브로커 주소
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        // Consumer 그룹 ID — 같은 그룹끼리 메시지를 나눠서 처리
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "coffee-order-group");
        // 메시지 key를 문자열로 역직렬화
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        // 메시지 value를 JSON으로 역직렬화 (JSON → 객체로 변환)
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        // Consumer가 처음 실행될 때 토픽의 맨 처음부터 메시지를 읽음
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        // 역직렬화 시 모든 패키지의 클래스를 신뢰 (없으면 보안 에러 발생)
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory() {
        // @KafkaListener가 메시지를 받을 때 사용하는 컨테이너 팩토리
        // 여러 메시지를 동시에(Concurrent) 처리할 수 있음
        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        return factory;
    }
}