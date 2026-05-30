package com.order_management_system.order_service.controllers;

import com.order_management_system.order_service.dto.OrderRequestDto;
import com.order_management_system.order_service.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/orders")
public class OrderController {
    @Autowired
    private OrderService orderService;

    @PostMapping
    public ResponseEntity<?> createOrder(
            @RequestHeader("Idempotency-Key") String key,
            @RequestBody OrderRequestDto request) {
        try{
            return orderService.createOrder(request,key);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }

    }
}
