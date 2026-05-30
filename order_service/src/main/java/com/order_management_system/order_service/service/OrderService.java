package com.order_management_system.order_service.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.order_management_system.order_service.dto.ErrorResponseDto;
import com.order_management_system.order_service.mapper.EventMapper;
import com.order_management_system.order_service.model.OrderCreatedEvent;
import com.order_management_system.order_service.producer.OrderEventProducer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.order_management_system.order_service.dto.OrderItemDto;
import com.order_management_system.order_service.dto.OrderRequestDto;
import com.order_management_system.order_service.dto.OrderResponseDto;
import com.order_management_system.order_service.repository.OrderRepository;
import com.order_management_system.order_service.repository.OrderItemRepository;
import com.order_management_system.order_service.model.Order;
import com.order_management_system.order_service.model.OrderItem;

@Service
public class OrderService {
    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository itemRepository;

    @Autowired
    private OrderRequestHasher orderRequestHasher;

    @Autowired
    private IdempotencyService idempotencyService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EventMapper eventMapper;

    @Autowired
    private OrderEventProducer orderEventProducer;

    public ResponseEntity<?> createOrder(OrderRequestDto request, String key) {

        String requestHash = orderRequestHasher.hash(request);

        if (idempotencyService.isConflictingKey(key, requestHash)) {
            ErrorResponseDto errorResponseDto = new ErrorResponseDto();
            errorResponseDto.setMessage("Idempotency key already used with different request body");
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(errorResponseDto);
        }

        Optional<String> existing = idempotencyService.checkDuplicate(key, requestHash);

        if (existing.isPresent()) {
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(existing.get());
        }
        UUID orderId = UUID.randomUUID();
        Order order = new Order();
        order.setOrderId(String.valueOf(orderId));
        order.setUserId(request.getUserId());
        order.setStatus("PENDING");
        order.setCreatedAt(LocalDateTime.now());
        orderRepository.save(order);

        List<OrderItem> savedItems = new ArrayList<>();
        for (OrderItemDto orderItems : request.getItems()) {
            OrderItem orderItem = new OrderItem();
            orderItem.setOrderId(String.valueOf(orderId));
            orderItem.setProductId(orderItems.getProductId());
            orderItem.setQuantity(orderItems.getQuantity());
            itemRepository.save(orderItem);
            savedItems.add(orderItem);
        }

        OrderCreatedEvent event = eventMapper.map(order, savedItems);
        orderEventProducer.publish(event);

        OrderResponseDto response = new OrderResponseDto();
        response.setOrderId(orderId);
        response.setStatus("PENDING");


        String responseJson;
        try {
            responseJson = objectMapper.writeValueAsString(response);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize order response for idempotency storage", e);
        }
        idempotencyService.save(key, requestHash, responseJson);
        return ResponseEntity.ok(response);
    }
}
