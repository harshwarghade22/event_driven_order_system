package com.order_management_system.order_service.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "idempotency_keys")
@Getter
@Setter
public class IdempotencyKey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String idempotencyKey;

    private String requestHash;

    @Lob
    private String response;

    private LocalDateTime createdAt;
}
