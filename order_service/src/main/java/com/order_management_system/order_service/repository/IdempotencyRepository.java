package com.order_management_system.order_service.repository;

import com.order_management_system.order_service.model.IdempotencyKey;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IdempotencyRepository extends JpaRepository<IdempotencyKey, Long> {
    Optional<IdempotencyKey> findByIdempotencyKey(String key);
}
