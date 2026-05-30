# Event-Driven Order Management System

A distributed order processing platform built with **Spring Boot 4** microservices, **Apache Kafka**, and **MySQL**. Orders are created synchronously via REST, then fulfilled asynchronously through an event-driven pipeline that reserves inventory, updates order status, and notifies users.

---

## Table of Contents

- [Overview](#overview)
- [Architecture](#architecture)
- [Services](#services)
- [Event Flow](#event-flow)
- [Kafka Topics](#kafka-topics)
- [Event Schemas](#event-schemas)
- [Order Lifecycle](#order-lifecycle)
- [Reliability Patterns](#reliability-patterns)
- [Tech Stack](#tech-stack)
- [Prerequisites](#prerequisites)
- [Getting Started](#getting-started)
- [API Reference](#api-reference)
- [Testing the Flow](#testing-the-flow)
- [Database Schema](#database-schema)
- [Project Structure](#project-structure)
- [Operational Notes](#operational-notes)
- [Design Decisions](#design-decisions)

---

## Overview

This system demonstrates a production-style **event-driven architecture** for order fulfillment:

1. A client submits an order to the **Order Service**.
2. The order is persisted with status `PENDING` and an `ORDER_CREATED` event is published to Kafka.
3. The **Inventory Service** consumes the event, reserves stock, and publishes either `INVENTORY_RESERVED` or `INVENTORY_FAILED`.
4. The **Order Service** listens for inventory outcomes and transitions the order to `CONFIRMED` or `FAILED`.
5. The **Notification Service** sends user-facing notifications (log-based in development) when inventory succeeds or fails.

Each service owns its database (**database-per-service** pattern). Communication between services happens exclusively through Kafka events — no direct HTTP calls between microservices.

---

## Architecture

```
┌─────────────┐     POST /orders      ┌──────────────────┐
│   Client    │ ────────────────────► │  Order Service   │ :8080
└─────────────┘                       │    (order_db)    │
                                      └────────┬─────────┘
                                               │ publish
                                               ▼
                                      ┌──────────────────┐
                                      │  order.created   │
                                      └────────┬─────────┘
                                               │ consume
                                               ▼
                                      ┌──────────────────┐
                                      │ Inventory Service│ :8081
                                      │  (inventory_db)  │
                                      └────────┬─────────┘
                           success           │           failure (via DLQ)
                    ┌─────────────────────────┼─────────────────────────┐
                    ▼                         │                         ▼
           ┌─────────────────┐                │              ┌──────────────────┐
           │inventory.reserved│               │              │ inventory.failed │
           └────────┬─────────┘                │              └────────┬─────────┘
                    │                          │                       │
         ┌──────────┴──────────┐               │            ┌──────────┴──────────┐
         ▼                     ▼               │            ▼                     ▼
┌──────────────────┐  ┌──────────────────┐    │   ┌──────────────────┐  ┌──────────────────┐
│  Order Service   │  │ Notification Svc │    │   │  Order Service   │  │ Notification Svc │
│   → CONFIRMED    │  │  → user notified │    │   │   → FAILED       │  │  → user notified │
└──────────────────┘  └──────────────────┘    │   └──────────────────┘  └──────────────────┘
                                              │
                                     ┌────────┴─────────┐
                                     │ order.created.dlq│  (transient errors / retries exhausted)
                                     └──────────────────┘
```

---

## Services

| Service | Port | Database | Responsibility |
|---------|------|----------|----------------|
| **order_service** | 8080 | `order_db` | REST API for order creation, idempotency, event publishing, status updates |
| **inventory_service** | 8081 | `inventory_db` | Stock validation & reservation, idempotent event processing, DLQ handling |
| **notification_service** | 8082 | `notification_db` | User notifications on order confirmation or failure |

---

## Event Flow

### Happy Path

```
POST /orders
  → Order saved (PENDING)
  → ORDER_CREATED published to order.created
  → Inventory reserves stock
  → INVENTORY_RESERVED published to inventory.reserved
  → Order status → CONFIRMED
  → Notification sent to user
```

### Failure Path (e.g. insufficient stock)

```
POST /orders
  → Order saved (PENDING)
  → ORDER_CREATED published to order.created
  → Inventory throws InsufficientStockException (non-retryable)
  → Message routed to order.created.dlq
  → DLQ consumer publishes INVENTORY_FAILED to inventory.failed
  → Order status → FAILED
  → Notification sent to user
```

---

## Kafka Topics

| Topic | Producer | Consumer(s) | Purpose |
|-------|----------|-------------|---------|
| `order.created` | order_service | inventory_service | New order events |
| `order.created.dlq` | inventory_service (DLQ recoverer) | inventory_service (DLQ monitor) | Failed order processing |
| `inventory.reserved` | inventory_service | order_service, notification_service | Successful stock reservation |
| `inventory.failed` | inventory_service (DLQ consumer) | order_service, notification_service | Failed stock reservation |

Topics are auto-created by Kafka on first publish. The included `docker-compose.yml` enables topic deletion for local development.

---

## Order Lifecycle

```
PENDING ──► CONFIRMED   (inventory.reserved received)
        └──► FAILED     (inventory.failed received)
```

- Orders start as `PENDING` immediately after creation.
- Status transitions are **asynchronous** — the API response always returns `PENDING`.
- Status updates only apply when the current status is `PENDING` (prevents overwriting terminal states on replay).

---

## Reliability Patterns

### Idempotency (Order Service)

- Clients must send an `Idempotency-Key` header with every `POST /orders` request.
- Same key + same request body → returns the cached response (HTTP 200).
- Same key + different body → HTTP 409 Conflict.

### Idempotency (Inventory Service)

- Uses an insert-first **claim pattern** on the `processed_events` table.
- Primary key: `event_id` (the `ORDER_CREATED` event ID).
- Unique constraint on `order_id` prevents duplicate processing of the same order.
- On processing failure, the claim is rolled back so Kafka retry can re-attempt.

### Idempotency (Notification Service)

- Uses `sent_notifications` table keyed by outbound event ID.
- Duplicate events are silently skipped.

### Retry & Dead Letter Queue (Inventory Service)

- Transient errors: retried up to **3 times** with **2-second** fixed backoff.
- Non-retryable errors (`ProductNotFoundException`, `InsufficientStockException`): immediately routed to `order.created.dlq`.
- DLQ consumer publishes a single `INVENTORY_FAILED` event (avoids duplicate failure notifications).

### Transactional Outbound Events (Inventory Service)

- `INVENTORY_RESERVED` is published **after database commit** via `TransactionSynchronization.afterCommit()`.
- Ensures events are only emitted when stock reservation is durably persisted.

---

## Tech Stack

| Layer | Technology |
|-------|------------|
| Language | Java 21 |
| Framework | Spring Boot 4.0.6 |
| Messaging | Apache Kafka (KRaft mode) |
| Database | MySQL 8 |
| ORM | Spring Data JPA / Hibernate |
| Serialization | Jackson 2 (JSON) |
| Build | Maven |
| Containerization | Docker Compose (Kafka) |

---

## Prerequisites

- **Java 21**
- **Maven 3.9+**
- **MySQL 8** running on `localhost:3306`
- **Docker** (for Kafka)

Update database credentials in each service's `application.properties` if your MySQL setup differs from the defaults (`root` / `harsh`).

---

## Getting Started

### 1. Start Kafka

```bash
cd event_driver_order_system
docker compose up -d
```

Verify Kafka is reachable on `localhost:9092`.

### 2. Start MySQL

Ensure MySQL is running. Databases (`order_db`, `inventory_db`, `notification_db`) are created automatically via `createDatabaseIfNotExist=true`.

### 3. Seed Inventory Products

Products are seeded via Flyway migration files (reference only — Flyway is disabled at runtime). Insert seed data manually or run:

```sql
USE inventory_db;

INSERT INTO products (product_id, name, stock_quantity) VALUES
    ('prod-1', 'Product One', 100),
    ('prod-2', 'Product Two', 50),
    ('prod-3', 'Product Three', 10);
```

Hibernate `ddl-auto=update` creates the `products` table on first inventory service startup.

### 4. Start the Microservices

Open three terminals:

```bash
# Terminal 1 — Order Service (port 8080)
cd order_service
./mvnw spring-boot:run

# Terminal 2 — Inventory Service (port 8081)
cd inventory_service
./mvnw spring-boot:run

# Terminal 3 — Notification Service (port 8082)
cd notification_service
./mvnw spring-boot:run
```

On Windows, use `mvnw.cmd` instead of `./mvnw`.

---

## API Reference

### Create Order

```
POST http://localhost:8080/orders
Content-Type: application/json
Idempotency-Key: <unique-key>
```

**Request body:**

```json
{
  "userId": "user-42",
  "items": [
    { "productId": "prod-1", "quantity": 2 },
    { "productId": "prod-2", "quantity": 1 }
  ]
}
```

**Success response (200):**

```json
{
  "orderId": "169acdbd-1234-5678-90ab-cdef12345678",
  "status": "PENDING"
}
```

**Conflict response (409):**

```json
{
  "message": "Idempotency key already used with different request body"
}
```

---

## Testing the Flow

### Successful Order

```bash
curl -X POST http://localhost:8080/orders \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: test-success-001" \
  -d '{
    "userId": "user-42",
    "items": [{ "productId": "prod-1", "quantity": 1 }]
  }'
```

**Expected logs:**

| Service | Log indicator |
|---------|---------------|
| order_service | Order created, event published |
| inventory_service | Stock reserved, `INVENTORY_RESERVED` published |
| order_service | Order status updated to `CONFIRMED` |
| notification_service | `NOTIFICATION SENT \| type=ORDER_CONFIRMED` |

### Failed Order (insufficient stock)

```bash
curl -X POST http://localhost:8080/orders \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: test-fail-001" \
  -d '{
    "userId": "user-42",
    "items": [{ "productId": "prod-3", "quantity": 999 }]
  }'
```

**Expected logs:**

| Service | Log indicator |
|---------|---------------|
| inventory_service | DLQ event received, `INVENTORY_FAILED` published |
| order_service | Order status updated to `FAILED` |
| notification_service | `NOTIFICATION SENT \| type=ORDER_FAILED` |

### Verify Order Status in Database

```sql
USE order_db;
SELECT order_id, user_id, status, created_at FROM orders ORDER BY created_at DESC LIMIT 5;
```

---

## Database Schema

Each service maintains its own schema. Hibernate manages table creation (`ddl-auto=update`).

### order_db

| Table | Purpose |
|-------|---------|
| `orders` | Order records (`order_id`, `user_id`, `status`, `created_at`) |
| `order_items` | Line items per order |
| `idempotency_records` | Idempotency key → response mapping |

### inventory_db

| Table | Purpose |
|-------|---------|
| `products` | Product catalog with stock quantities |
| `processed_events` | Idempotency ledger (`event_id` PK, `order_id` UNIQUE) |

### notification_db

| Table | Purpose |
|-------|---------|
| `sent_notifications` | Deduplication ledger for sent notifications |

---

## Operational Notes

### Resetting Kafka Topics

If stale messages cause unexpected replays during development:

```powershell
# Stop inventory_service first, then:
.\scripts\reset-order-created-topic.ps1
```

Optionally clear idempotency state:

```sql
TRUNCATE TABLE inventory_db.processed_events;
TRUNCATE TABLE notification_db.sent_notifications;
```

### Flyway Migrations

Flyway is **disabled** at runtime (`spring.flyway.enabled=false`). SQL migration files under `src/main/resources/db/migration/` serve as schema reference. Hibernate `ddl-auto=update` handles table creation.

### Consumer Groups

| Service | Consumer Group | Topics |
|---------|---------------|--------|
| inventory_service | `inventory-service-group` | `order.created` |
| inventory_service | `inventory-dlq-monitor` | `order.created.dlq` |
| order_service | `order-service-group` | `inventory.reserved`, `inventory.failed` |
| notification_service | `notification-service-group` | `inventory.reserved`, `inventory.failed` |

---

## Design Decisions

| Decision | Rationale |
|----------|-----------|
| **Database per service** | Independent scaling, schema evolution, and failure isolation |
| **Async status updates** | Decouples order creation latency from inventory processing |
| **Kafka as integration backbone** | Durable, replayable event log with natural fan-out to multiple consumers |
| **Insert-first idempotency** | Simple, race-safe deduplication without distributed locks |
| **DLQ for non-retryable failures** | Separates business failures from infrastructure retries; single failure event publisher |
| **afterCommit event publishing** | Prevents phantom events if the database transaction rolls back |
| **Log-based notifications** | Keeps the notification service focused on event handling; swap in email/SMS/push later |
| **PENDING-only status transitions** | Idempotent replays cannot overwrite terminal order states |

---

## License

This project is provided for educational and demonstration purposes.
