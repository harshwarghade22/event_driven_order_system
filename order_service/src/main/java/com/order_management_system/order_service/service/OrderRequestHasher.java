package com.order_management_system.order_service.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.order_management_system.order_service.dto.OrderRequestDto;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Component

public class OrderRequestHasher {

    private final ObjectMapper canonicalMapper;

    public OrderRequestHasher(ObjectMapper objectMapper) {
        this.canonicalMapper = objectMapper.copy();
    }

    /**
     * Stable fingerprint of the request body for idempotency: canonical JSON + SHA-256 (hex).
     */
    public String hash(OrderRequestDto request) {
        try {
            String json = canonicalMapper.writeValueAsString(request);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(json.getBytes(StandardCharsets.UTF_8)));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize order request for hashing", e);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
