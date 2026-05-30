package com.order_management_system.inventory_service.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class KafkaTopicProperties {

    @Value("${inventory.kafka.topic.order-created:order.created}")
    private String orderCreated;

    @Value("${inventory.kafka.topic.order-created-dlq:order.created.dlq}")
    private String orderCreatedDlq;

    @Value("${inventory.kafka.topic.inventory-reserved:inventory.reserved}")
    private String inventoryReserved;

    @Value("${inventory.kafka.topic.inventory-failed:inventory.failed}")
    private String inventoryFailed;

    public String getOrderCreated() {
        return orderCreated;
    }

    public String getOrderCreatedDlq() {
        return orderCreatedDlq;
    }

    public String getInventoryReserved() {
        return inventoryReserved;
    }

    public String getInventoryFailed() {
        return inventoryFailed;
    }
}
