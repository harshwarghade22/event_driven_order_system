package com.order_management_system.notification_service.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
public class InventoryFailedEvent {
    private UUID eventId;
    private String eventType;
    private String version;
    private LocalDateTime timestamp;

    private UUID orderId;
    private UUID correlationEventId;
    private String userId;
    private String failureType;
    private String reason;
}
