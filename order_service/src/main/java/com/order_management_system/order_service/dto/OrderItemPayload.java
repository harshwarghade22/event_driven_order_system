package com.order_management_system.order_service.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderItemPayload {
    private String productId;
    private int quantity;
}
