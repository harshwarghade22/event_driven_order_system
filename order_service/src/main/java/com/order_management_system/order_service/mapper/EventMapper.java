package com.order_management_system.order_service.mapper;

import com.order_management_system.order_service.dto.OrderItemPayload;
import com.order_management_system.order_service.model.Order;
import com.order_management_system.order_service.model.OrderCreatedEvent;
import com.order_management_system.order_service.model.OrderItem;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Component
public class EventMapper {

    public OrderCreatedEvent map(Order order, List<OrderItem> items) {

        OrderCreatedEvent event = new OrderCreatedEvent();

        event.setEventId(UUID.randomUUID());
        event.setEventType("ORDER_CREATED");
        event.setVersion("v1");
        event.setTimestamp(LocalDateTime.now());

        event.setOrderId(UUID.fromString(order.getOrderId()));
        event.setUserId(order.getUserId());

        List<OrderItemPayload> payloads = items.stream()
                .map(i -> {
                    OrderItemPayload p = new OrderItemPayload();
                    p.setProductId(i.getProductId());
                    p.setQuantity(i.getQuantity());
                    return p;
                })
                .toList();

        event.setItems(payloads);

        return event;
    }
}
