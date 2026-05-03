package com.order_management_system.order_service.dto;

import java.util.UUID;

import jakarta.annotation.Generated;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderResponseDto {
    private UUID orderId;
    private String status;
}
