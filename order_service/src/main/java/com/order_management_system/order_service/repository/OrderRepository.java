package com.order_management_system.order_service.repository;

import com.order_management_system.order_service.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {

}
