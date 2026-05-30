package com.order_management_system.notification_service.repository;

import com.order_management_system.notification_service.model.SentNotification;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SentNotificationRepository extends JpaRepository<SentNotification, String> {
}
