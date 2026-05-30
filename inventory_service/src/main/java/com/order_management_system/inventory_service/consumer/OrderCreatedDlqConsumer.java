package com.order_management_system.inventory_service.consumer;

import com.order_management_system.inventory_service.dto.OrderCreatedEvent;
import com.order_management_system.inventory_service.mapper.InventoryEventMapper;
import com.order_management_system.inventory_service.producer.InventoryEventProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Component
public class OrderCreatedDlqConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderCreatedDlqConsumer.class);

    private final InventoryEventMapper inventoryEventMapper;
    private final InventoryEventProducer inventoryEventProducer;

    public OrderCreatedDlqConsumer(
            InventoryEventMapper inventoryEventMapper,
            InventoryEventProducer inventoryEventProducer) {
        this.inventoryEventMapper = inventoryEventMapper;
        this.inventoryEventProducer = inventoryEventProducer;
    }

    @KafkaListener(
            topics = "${inventory.kafka.topic.order-created-dlq:order.created.dlq}",
            groupId = "inventory-dlq-monitor",
            containerFactory = "dlqKafkaListenerContainerFactory"
    )
    public void consumeDlq(
            OrderCreatedEvent event,
            @Header(KafkaHeaders.RECEIVED_KEY) String key,
            @Header(value = KafkaHeaders.DLT_EXCEPTION_MESSAGE, required = false) String errorMessage,
            @Header(value = KafkaHeaders.DLT_EXCEPTION_FQCN, required = false) String exceptionClass) {
        log.error(
                "DLQ event received: eventId={}, orderId={}, key={}, exceptionClass={}, error={}",
                event.getEventId(),
                event.getOrderId(),
                key,
                exceptionClass,
                errorMessage
        );

        inventoryEventProducer.publishFailed(
                inventoryEventMapper.toFailedEvent(event, exceptionClass, errorMessage)
        );
    }
}
