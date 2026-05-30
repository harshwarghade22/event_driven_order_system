package com.order_management_system.notification_service.service;

import com.order_management_system.notification_service.dto.InventoryFailedEvent;
import com.order_management_system.notification_service.dto.InventoryReservedEvent;
import com.order_management_system.notification_service.model.SentNotification;
import com.order_management_system.notification_service.repository.SentNotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final SentNotificationRepository sentNotificationRepository;

    public NotificationService(SentNotificationRepository sentNotificationRepository) {
        this.sentNotificationRepository = sentNotificationRepository;
    }

    @Transactional
    public void notifyOrderConfirmed(InventoryReservedEvent event) {
        if (!claimNotification(event.getEventId().toString(), event.getEventType(), event.getOrderId().toString(), event.getUserId())) {
            log.info("Notification already sent, skipping: eventId={}", event.getEventId());
            return;
        }

        log.info(
                "NOTIFICATION SENT | type=ORDER_CONFIRMED | userId={} | orderId={} | message=Your order {} has been confirmed and inventory is reserved.",
                event.getUserId(),
                event.getOrderId(),
                event.getOrderId()
        );
    }

    @Transactional
    public void notifyOrderFailed(InventoryFailedEvent event) {
        if (!claimNotification(event.getEventId().toString(), event.getEventType(), event.getOrderId().toString(), event.getUserId())) {
            log.info("Notification already sent, skipping: eventId={}", event.getEventId());
            return;
        }

        log.info(
                "NOTIFICATION SENT | type=ORDER_FAILED | userId={} | orderId={} | failureType={} | message=Your order {} could not be fulfilled. Reason: {}",
                event.getUserId(),
                event.getOrderId(),
                event.getFailureType(),
                event.getOrderId(),
                event.getReason()
        );
    }

    private boolean claimNotification(String eventId, String eventType, String orderId, String userId) {
        SentNotification record = new SentNotification();
        record.setEventId(eventId);
        record.setEventType(eventType);
        record.setOrderId(orderId);
        record.setUserId(userId);
        record.setSentAt(LocalDateTime.now());

        try {
            sentNotificationRepository.saveAndFlush(record);
            return true;
        } catch (DataIntegrityViolationException ex) {
            return false;
        }
    }
}
