package org.beckn.catalog;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;

@EnableKafka
@SpringBootApplication
public class CatalogPublishApplication {
    public static void main(String[] args) {
        SpringApplication.run(CatalogPublishApplication.class, args);
    }
}
