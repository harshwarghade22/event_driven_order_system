package com.order_management_system.notification_service.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class InventoryReservedEvent {
    private UUID eventId;
    private String eventType;
    private String version;
    private LocalDateTime timestamp;

    private UUID orderId;
    private UUID correlationEventId;
    private String userId;

    private List<OrderItemPayload> items;
}
