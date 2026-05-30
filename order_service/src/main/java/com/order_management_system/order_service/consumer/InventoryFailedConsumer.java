package com.order_management_system.order_service.consumer;

import com.order_management_system.order_service.dto.InventoryFailedEvent;
import com.order_management_system.order_service.service.OrderStatusService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class InventoryFailedConsumer {

    private static final Logger log = LoggerFactory.getLogger(InventoryFailedConsumer.class);

    private final OrderStatusService orderStatusService;

    public InventoryFailedConsumer(OrderStatusService orderStatusService) {
        this.orderStatusService = orderStatusService;
    }

    @KafkaListener(
            topics = "${order.kafka.topic.inventory-failed:inventory.failed}",
            groupId = "order-service-group",
            containerFactory = "inventoryFailedKafkaListenerContainerFactory"
    )
    public void consume(InventoryFailedEvent event) {
        log.info(
                "Received INVENTORY_FAILED: eventId={}, orderId={}, failureType={}, reason={}",
                event.getEventId(),
                event.getOrderId(),
                event.getFailureType(),
                event.getReason()
        );

        orderStatusService.failOrder(event.getOrderId());
    }
}
