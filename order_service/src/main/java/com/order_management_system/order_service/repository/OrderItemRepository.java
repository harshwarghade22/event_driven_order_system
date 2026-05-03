package com.order_management_system.order_service.repository;

import com.order_management_system.order_service.model.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
}
