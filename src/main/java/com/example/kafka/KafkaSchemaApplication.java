package com.example.kafka;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;

@EnableKafka
@SpringBootApplication
public class KafkaSchemaApplication {
    public static void main(String[] args) {
        SpringApplication.run(KafkaSchemaApplication.class, args);
    }
}
