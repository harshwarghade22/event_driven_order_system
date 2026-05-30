package com.order_management_system.notification_service.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.order_management_system.notification_service.dto.InventoryFailedEvent;
import com.order_management_system.notification_service.dto.InventoryReservedEvent;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConsumerConfig {

    @Bean
    public ConsumerFactory<String, InventoryReservedEvent> inventoryReservedEventConsumerFactory(
            KafkaProperties kafkaProperties,
            ObjectMapper objectMapper) {
        return buildConsumerFactory(kafkaProperties, objectMapper, InventoryReservedEvent.class);
    }

    @Bean
    public ConsumerFactory<String, InventoryFailedEvent> inventoryFailedEventConsumerFactory(
            KafkaProperties kafkaProperties,
            ObjectMapper objectMapper) {
        return buildConsumerFactory(kafkaProperties, objectMapper, InventoryFailedEvent.class);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, InventoryReservedEvent> inventoryReservedKafkaListenerContainerFactory(
            ConsumerFactory<String, InventoryReservedEvent> inventoryReservedEventConsumerFactory) {
        ConcurrentKafkaListenerContainerFactory<String, InventoryReservedEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(inventoryReservedEventConsumerFactory);
        return factory;
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, InventoryFailedEvent> inventoryFailedKafkaListenerContainerFactory(
            ConsumerFactory<String, InventoryFailedEvent> inventoryFailedEventConsumerFactory) {
        ConcurrentKafkaListenerContainerFactory<String, InventoryFailedEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(inventoryFailedEventConsumerFactory);
        return factory;
    }

    private <T> ConsumerFactory<String, T> buildConsumerFactory(
            KafkaProperties kafkaProperties,
            ObjectMapper objectMapper,
            Class<T> eventType) {
        Map<String, Object> config = new HashMap<>(kafkaProperties.buildConsumerProperties());
        JsonDeserializer<T> deserializer = new JsonDeserializer<>(eventType, objectMapper);
        deserializer.setUseTypeHeaders(false);
        deserializer.addTrustedPackages("*");
        return new DefaultKafkaConsumerFactory<>(config, new StringDeserializer(), deserializer);
    }
}
