package com.order_management_system.order_service.service;

import com.order_management_system.order_service.model.Order;
import com.order_management_system.order_service.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class OrderStatusService {

    private static final Logger log = LoggerFactory.getLogger(OrderStatusService.class);

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_CONFIRMED = "CONFIRMED";
    public static final String STATUS_FAILED = "FAILED";

    private final OrderRepository orderRepository;

    public OrderStatusService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Transactional
    public void confirmOrder(UUID orderId) {
        updateStatus(orderId, STATUS_CONFIRMED);
    }

    @Transactional
    public void failOrder(UUID orderId) {
        updateStatus(orderId, STATUS_FAILED);
    }

    private void updateStatus(UUID orderId, String newStatus) {
        String orderIdValue = orderId.toString();

        Order order = orderRepository.findById(orderIdValue).orElse(null);
        if (order == null) {
            log.warn("Order not found for status update: orderId={}, targetStatus={}", orderIdValue, newStatus);
            return;
        }

        if (!STATUS_PENDING.equals(order.getStatus())) {
            log.info(
                    "Skipping status update for orderId={}, currentStatus={}, targetStatus={}",
                    orderIdValue,
                    order.getStatus(),
                    newStatus
            );
            return;
        }

        order.setStatus(newStatus);
        orderRepository.save(order);

        log.info("Updated order status: orderId={}, status={}", orderIdValue, newStatus);
    }
}
