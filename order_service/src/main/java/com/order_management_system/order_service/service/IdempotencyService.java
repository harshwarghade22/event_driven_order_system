package com.order_management_system.order_service.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.order_management_system.order_service.model.IdempotencyKey;
import com.order_management_system.order_service.repository.IdempotencyRepository;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

@Service
public class IdempotencyService {
    @Autowired
    private IdempotencyRepository idempotencyRepository;

    public Optional<String> checkDuplicate(String key, String requestHash) {
        Optional<IdempotencyKey> record = idempotencyRepository.findByIdempotencyKey(key);

        if (record.isPresent() && Objects.equals(record.get().getRequestHash(), requestHash)) {
            return Optional.of(record.get().getResponse());
        }
        return Optional.empty();
    }

    /**
     * Same idempotency key was already used with a different request body.
     */
    public boolean isConflictingKey(String key, String requestHash) {
        return idempotencyRepository.findByIdempotencyKey(key)
                .map(r -> !Objects.equals(r.getRequestHash(), requestHash))
                .orElse(false);
    }

    public void save(String key, String requestHash, String response) {
        IdempotencyKey entity = new IdempotencyKey();
        entity.setIdempotencyKey(key);
        entity.setRequestHash(requestHash);
        entity.setResponse(response);
        entity.setCreatedAt(LocalDateTime.now());

        idempotencyRepository.save(entity);
    }
}
