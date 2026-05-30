package com.order_management_system.inventory_service.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class OrderItemPayload {
    private String productId;
    private int quantity;
}
