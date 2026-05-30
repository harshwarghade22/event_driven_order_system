package com.order_management_system.notification_service.consumer;

import com.order_management_system.notification_service.dto.InventoryReservedEvent;
import com.order_management_system.notification_service.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class InventoryReservedConsumer {

    private static final Logger log = LoggerFactory.getLogger(InventoryReservedConsumer.class);

    private final NotificationService notificationService;

    public InventoryReservedConsumer(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @KafkaListener(
            topics = "${notification.kafka.topic.inventory-reserved:inventory.reserved}",
            groupId = "notification-service-group",
            containerFactory = "inventoryReservedKafkaListenerContainerFactory"
    )
    public void consume(InventoryReservedEvent event) {
        log.info(
                "Received INVENTORY_RESERVED: eventId={}, orderId={}, userId={}",
                event.getEventId(),
                event.getOrderId(),
                event.getUserId()
        );

        notificationService.notifyOrderConfirmed(event);
    }
}
