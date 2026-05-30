package com.order_management_system.inventory_service.repository;

import com.order_management_system.inventory_service.model.ProcessedEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, String> {

    boolean existsByOrderId(String orderId);
}
