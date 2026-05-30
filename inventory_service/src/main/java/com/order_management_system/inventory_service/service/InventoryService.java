package com.order_management_system.inventory_service.service;

import com.order_management_system.inventory_service.dto.InventoryReservedEvent;
import com.order_management_system.inventory_service.dto.OrderCreatedEvent;
import com.order_management_system.inventory_service.dto.OrderItemPayload;
import com.order_management_system.inventory_service.exception.InsufficientStockException;
import com.order_management_system.inventory_service.exception.ProductNotFoundException;
import com.order_management_system.inventory_service.mapper.InventoryEventMapper;
import com.order_management_system.inventory_service.model.ProcessedEvent;
import com.order_management_system.inventory_service.model.Product;
import com.order_management_system.inventory_service.producer.InventoryEventProducer;
import com.order_management_system.inventory_service.repository.ProcessedEventRepository;
import com.order_management_system.inventory_service.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class InventoryService {

    private static final Logger log = LoggerFactory.getLogger(InventoryService.class);

    private final ProductRepository productRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final InventoryEventMapper inventoryEventMapper;
    private final InventoryEventProducer inventoryEventProducer;

    public InventoryService(
            ProductRepository productRepository,
            ProcessedEventRepository processedEventRepository,
            InventoryEventMapper inventoryEventMapper,
            InventoryEventProducer inventoryEventProducer) {
        this.productRepository = productRepository;
        this.processedEventRepository = processedEventRepository;
        this.inventoryEventMapper = inventoryEventMapper;
        this.inventoryEventProducer = inventoryEventProducer;
    }

    @Transactional
    public void processOrderCreatedEvent(OrderCreatedEvent event) {
        String eventId = event.getEventId().toString();
        String orderId = event.getOrderId().toString();

        if (processedEventRepository.existsById(eventId)
                || processedEventRepository.existsByOrderId(orderId)) {
            log.info("Event already processed, skipping: eventId={}, orderId={}", eventId, orderId);
            return;
        }

        if (!claimEvent(event, eventId, orderId)) {
            log.info("Event already processed, skipping: eventId={}, orderId={}", eventId, orderId);
            return;
        }

        try {
            reserveStock(event.getOrderId(), event.getItems());
            publishReservedAfterCommit(event);
        } catch (RuntimeException ex) {
            processedEventRepository.deleteById(eventId);
            throw ex;
        }
    }

    private void publishReservedAfterCommit(OrderCreatedEvent source) {
        InventoryReservedEvent reservedEvent = inventoryEventMapper.toReservedEvent(source);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                inventoryEventProducer.publishReserved(reservedEvent);
            }
        });
    }

    private boolean claimEvent(OrderCreatedEvent event, String eventId, String orderId) {
        ProcessedEvent processed = new ProcessedEvent();
        processed.setEventId(eventId);
        processed.setEventType(event.getEventType());
        processed.setOrderId(orderId);
        processed.setProcessedAt(LocalDateTime.now());

        try {
            processedEventRepository.saveAndFlush(processed);
            return true;
        } catch (DataIntegrityViolationException ex) {
            return false;
        }
    }

    private void reserveStock(UUID orderId, List<OrderItemPayload> items) {
        for (OrderItemPayload item : items) {
            Product product = productRepository.findByProductIdForUpdate(item.getProductId())
                    .orElseThrow(() -> new ProductNotFoundException(item.getProductId()));

            if (product.getStockQuantity() < item.getQuantity()) {
                throw new InsufficientStockException(
                        item.getProductId(),
                        item.getQuantity(),
                        product.getStockQuantity()
                );
            }

            product.setStockQuantity(product.getStockQuantity() - item.getQuantity());
            productRepository.save(product);

            log.info(
                    "Reserved stock for orderId={}, productId={}, quantity={}, remaining={}",
                    orderId,
                    item.getProductId(),
                    item.getQuantity(),
                    product.getStockQuantity()
            );
        }

        log.info("Stock reservation completed for orderId={}", orderId);
    }
}
