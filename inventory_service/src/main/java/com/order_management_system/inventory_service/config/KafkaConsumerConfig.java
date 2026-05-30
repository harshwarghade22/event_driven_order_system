package com.order_management_system.inventory_service.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.order_management_system.inventory_service.dto.OrderCreatedEvent;
import com.order_management_system.inventory_service.exception.InsufficientStockException;
import com.order_management_system.inventory_service.exception.ProductNotFoundException;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConsumerConfig {

    @Value("${inventory.kafka.retry.backoff-ms:2000}")
    private long retryBackoffMs;

    @Value("${inventory.kafka.retry.max-attempts:3}")
    private long maxRetryAttempts;

    @Bean
    public ConsumerFactory<String, OrderCreatedEvent> orderCreatedEventConsumerFactory(
            KafkaProperties kafkaProperties,
            ObjectMapper objectMapper) {
        Map<String, Object> config = new HashMap<>(kafkaProperties.buildConsumerProperties());
        JsonDeserializer<OrderCreatedEvent> deserializer =
                new JsonDeserializer<>(OrderCreatedEvent.class, objectMapper);
        deserializer.setUseTypeHeaders(false);
        deserializer.addTrustedPackages("*");
        return new DefaultKafkaConsumerFactory<>(config, new StringDeserializer(), deserializer);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, OrderCreatedEvent> orderCreatedKafkaListenerContainerFactory(
            ConsumerFactory<String, OrderCreatedEvent> orderCreatedEventConsumerFactory,
            KafkaTemplate<String, OrderCreatedEvent> kafkaTemplate,
            KafkaTopicProperties kafkaTopicProperties) {
        ConcurrentKafkaListenerContainerFactory<String, OrderCreatedEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(orderCreatedEventConsumerFactory);
        factory.setCommonErrorHandler(orderCreatedErrorHandler(kafkaTemplate, kafkaTopicProperties));
        return factory;
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, OrderCreatedEvent> dlqKafkaListenerContainerFactory(
            ConsumerFactory<String, OrderCreatedEvent> orderCreatedEventConsumerFactory) {
        ConcurrentKafkaListenerContainerFactory<String, OrderCreatedEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(orderCreatedEventConsumerFactory);
        return factory;
    }

    private DefaultErrorHandler orderCreatedErrorHandler(
            KafkaTemplate<String, OrderCreatedEvent> kafkaTemplate,
            KafkaTopicProperties kafkaTopicProperties) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                kafkaTemplate,
                (record, ex) -> new TopicPartition(
                        kafkaTopicProperties.getOrderCreatedDlq(),
                        record.partition()
                )
        );

        DefaultErrorHandler errorHandler = new DefaultErrorHandler(
                recoverer,
                new FixedBackOff(retryBackoffMs, maxRetryAttempts)
        );

        errorHandler.addNotRetryableExceptions(
                ProductNotFoundException.class,
                InsufficientStockException.class
        );

        return errorHandler;
    }
}
