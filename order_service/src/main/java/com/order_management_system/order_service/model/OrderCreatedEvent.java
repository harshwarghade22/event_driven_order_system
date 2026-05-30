package com.order_management_system.order_service.model;

import com.order_management_system.order_service.dto.OrderItemPayload;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class OrderCreatedEvent {
    private UUID eventId;
    private String eventType;
    private String version;
    private LocalDateTime timestamp;

    private UUID orderId;
    private String userId;

    private List<OrderItemPayload> items;
}
