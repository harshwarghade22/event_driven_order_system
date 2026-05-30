package com.order_management_system.notification_service.consumer;

import com.order_management_system.notification_service.dto.InventoryFailedEvent;
import com.order_management_system.notification_service.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class InventoryFailedConsumer {

    private static final Logger log = LoggerFactory.getLogger(InventoryFailedConsumer.class);

    private final NotificationService notificationService;

    public InventoryFailedConsumer(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @KafkaListener(
            topics = "${notification.kafka.topic.inventory-failed:inventory.failed}",
            groupId = "notification-service-group",
            containerFactory = "inventoryFailedKafkaListenerContainerFactory"
    )
    public void consume(InventoryFailedEvent event) {
        log.info(
                "Received INVENTORY_FAILED: eventId={}, orderId={}, userId={}, reason={}",
                event.getEventId(),
                event.getOrderId(),
                event.getUserId(),
                event.getReason()
        );

        notificationService.notifyOrderFailed(event);
    }
}
