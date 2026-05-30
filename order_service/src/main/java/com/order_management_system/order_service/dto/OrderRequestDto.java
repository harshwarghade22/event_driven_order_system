package com.order_management_system.order_service.dto;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderRequestDto {
    private String userId;
    private List<OrderItemDto> items;
}
