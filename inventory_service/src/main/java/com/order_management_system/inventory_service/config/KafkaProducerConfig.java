package com.order_management_system.inventory_service.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.order_management_system.inventory_service.dto.OrderCreatedEvent;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaProducerConfig {

    @Bean
    public ProducerFactory<String, OrderCreatedEvent> orderCreatedEventProducerFactory(
            KafkaProperties kafkaProperties,
            ObjectMapper objectMapper) {
        Map<String, Object> config = new HashMap<>(kafkaProperties.buildProducerProperties());
        JsonSerializer<OrderCreatedEvent> serializer = new JsonSerializer<>(objectMapper);
        serializer.setAddTypeInfo(false);
        return new DefaultKafkaProducerFactory<>(config, new StringSerializer(), serializer);
    }

    @Bean
    public KafkaTemplate<String, OrderCreatedEvent> orderCreatedEventKafkaTemplate(
            ProducerFactory<String, OrderCreatedEvent> orderCreatedEventProducerFactory) {
        return new KafkaTemplate<>(orderCreatedEventProducerFactory);
    }

    @Bean
    public ProducerFactory<String, Object> inventoryEventProducerFactory(
            KafkaProperties kafkaProperties,
            ObjectMapper objectMapper) {
        Map<String, Object> config = new HashMap<>(kafkaProperties.buildProducerProperties());
        JsonSerializer<Object> serializer = new JsonSerializer<>(objectMapper);
        serializer.setAddTypeInfo(false);
        return new DefaultKafkaProducerFactory<>(config, new StringSerializer(), serializer);
    }

    @Bean
    public KafkaTemplate<String, Object> inventoryEventKafkaTemplate(
            ProducerFactory<String, Object> inventoryEventProducerFactory) {
        return new KafkaTemplate<>(inventoryEventProducerFactory);
    }
}
