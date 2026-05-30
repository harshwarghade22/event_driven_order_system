package com.order_management_system.inventory_service.producer;

import com.order_management_system.inventory_service.config.KafkaTopicProperties;
import com.order_management_system.inventory_service.dto.InventoryFailedEvent;
import com.order_management_system.inventory_service.dto.InventoryReservedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class InventoryEventProducer {

    private static final Logger log = LoggerFactory.getLogger(InventoryEventProducer.class);

    private final KafkaTemplate<String, Object> inventoryEventKafkaTemplate;
    private final KafkaTopicProperties kafkaTopicProperties;

    public InventoryEventProducer(
            KafkaTemplate<String, Object> inventoryEventKafkaTemplate,
            KafkaTopicProperties kafkaTopicProperties) {
        this.inventoryEventKafkaTemplate = inventoryEventKafkaTemplate;
        this.kafkaTopicProperties = kafkaTopicProperties;
    }

    public void publishReserved(InventoryReservedEvent event) {
        inventoryEventKafkaTemplate.send(
                kafkaTopicProperties.getInventoryReserved(),
                event.getOrderId().toString(),
                event
        );
        log.info(
                "Published INVENTORY_RESERVED: eventId={}, orderId={}, correlationEventId={}",
                event.getEventId(),
                event.getOrderId(),
                event.getCorrelationEventId()
        );
    }

    public void publishFailed(InventoryFailedEvent event) {
        inventoryEventKafkaTemplate.send(
                kafkaTopicProperties.getInventoryFailed(),
                event.getOrderId().toString(),
                event
        );
        log.info(
                "Published INVENTORY_FAILED: eventId={}, orderId={}, failureType={}, reason={}",
                event.getEventId(),
                event.getOrderId(),
                event.getFailureType(),
                event.getReason()
        );
    }
}
