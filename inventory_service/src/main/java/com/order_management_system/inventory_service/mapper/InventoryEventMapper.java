package com.order_management_system.inventory_service.mapper;

import com.order_management_system.inventory_service.dto.InventoryFailedEvent;
import com.order_management_system.inventory_service.dto.InventoryReservedEvent;
import com.order_management_system.inventory_service.dto.OrderCreatedEvent;
import com.order_management_system.inventory_service.exception.InsufficientStockException;
import com.order_management_system.inventory_service.exception.ProductNotFoundException;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
public class InventoryEventMapper {

    public InventoryReservedEvent toReservedEvent(OrderCreatedEvent source) {
        InventoryReservedEvent event = new InventoryReservedEvent();
        event.setEventId(UUID.randomUUID());
        event.setEventType("INVENTORY_RESERVED");
        event.setVersion("v1");
        event.setTimestamp(LocalDateTime.now());
        event.setOrderId(source.getOrderId());
        event.setCorrelationEventId(source.getEventId());
        event.setUserId(source.getUserId());
        event.setItems(source.getItems());
        return event;
    }

    public InventoryFailedEvent toFailedEvent(OrderCreatedEvent source, Exception ex) {
        return toFailedEvent(source, ex.getClass().getName(), ex.getMessage());
    }

    public InventoryFailedEvent toFailedEvent(
            OrderCreatedEvent source,
            String exceptionClass,
            String errorMessage) {
        InventoryFailedEvent event = new InventoryFailedEvent();
        event.setEventId(UUID.randomUUID());
        event.setEventType("INVENTORY_FAILED");
        event.setVersion("v1");
        event.setTimestamp(LocalDateTime.now());
        event.setOrderId(source.getOrderId());
        event.setCorrelationEventId(source.getEventId());
        event.setUserId(source.getUserId());
        event.setFailureType(resolveFailureType(exceptionClass, errorMessage));
        event.setReason(errorMessage != null ? errorMessage : "Processing failed");
        return event;
    }

    private String resolveFailureType(String exceptionClass, String errorMessage) {
        if (exceptionClass != null) {
            if (exceptionClass.contains(ProductNotFoundException.class.getSimpleName())) {
                return "PRODUCT_NOT_FOUND";
            }
            if (exceptionClass.contains(InsufficientStockException.class.getSimpleName())) {
                return "INSUFFICIENT_STOCK";
            }
        }
        if (errorMessage != null) {
            if (errorMessage.contains("Product not found")) {
                return "PRODUCT_NOT_FOUND";
            }
            if (errorMessage.contains("Insufficient stock")) {
                return "INSUFFICIENT_STOCK";
            }
        }
        return "PROCESSING_ERROR";
    }
}
