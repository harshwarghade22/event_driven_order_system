package com.order_management_system.order_service.dto;

import jakarta.annotation.Generated;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderItemDto {
    private String productId;
    private Integer quantity;
}
