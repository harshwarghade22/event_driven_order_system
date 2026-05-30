package com.order_management_system.order_service.consumer;

import com.order_management_system.order_service.dto.InventoryReservedEvent;
import com.order_management_system.order_service.service.OrderStatusService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class InventoryReservedConsumer {

    private static final Logger log = LoggerFactory.getLogger(InventoryReservedConsumer.class);

    private final OrderStatusService orderStatusService;

    public InventoryReservedConsumer(OrderStatusService orderStatusService) {
        this.orderStatusService = orderStatusService;
    }

    @KafkaListener(
            topics = "${order.kafka.topic.inventory-reserved:inventory.reserved}",
            groupId = "order-service-group",
            containerFactory = "inventoryReservedKafkaListenerContainerFactory"
    )
    public void consume(InventoryReservedEvent event) {
        log.info(
                "Received INVENTORY_RESERVED: eventId={}, orderId={}, correlationEventId={}",
                event.getEventId(),
                event.getOrderId(),
                event.getCorrelationEventId()
        );

        orderStatusService.confirmOrder(event.getOrderId());
    }
}
