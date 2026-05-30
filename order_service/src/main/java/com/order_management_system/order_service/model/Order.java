package com.order_management_system.order_service.model;



import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "orders")
@Getter
@Setter
public class Order {

    @Id
    private String orderId;

    private String userId;

    private String status;

    private BigDecimal totalAmount;

    private LocalDateTime createdAt;

}
