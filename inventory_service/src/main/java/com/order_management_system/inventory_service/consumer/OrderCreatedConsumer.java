package com.order_management_system.inventory_service.consumer;

import com.order_management_system.inventory_service.dto.OrderCreatedEvent;
import com.order_management_system.inventory_service.service.InventoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class OrderCreatedConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderCreatedConsumer.class);

    private final InventoryService inventoryService;

    public OrderCreatedConsumer(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @KafkaListener(
            topics = "${inventory.kafka.topic.order-created:order.created}",
            groupId = "inventory-service-group",
            containerFactory = "orderCreatedKafkaListenerContainerFactory"
    )
    public void consume(OrderCreatedEvent event) {
        log.info(
                "Received ORDER_CREATED event: eventId={}, orderId={}, userId={}, items={}",
                event.getEventId(),
                event.getOrderId(),
                event.getUserId(),
                event.getItems()
        );

        inventoryService.processOrderCreatedEvent(event);
    }
}
