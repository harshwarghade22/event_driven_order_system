package com.order_management_system.notification_service.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "sent_notifications")
@Getter
@Setter
public class SentNotification {

    @Id
    @Column(name = "event_id", length = 36, nullable = false)
    private String eventId;

    @Column(name = "event_type", length = 50, nullable = false)
    private String eventType;

    @Column(name = "order_id", length = 36, nullable = false)
    private String orderId;

    @Column(name = "user_id", length = 255)
    private String userId;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;
}
