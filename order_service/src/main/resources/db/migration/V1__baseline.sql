CREATE TABLE orders (
                        order_id CHAR(36) PRIMARY KEY,
                        user_id CHAR(36) NOT NULL,
                        status VARCHAR(20) NOT NULL,
                        total_amount DECIMAL(10,2),
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE order_items (
                             id BIGINT AUTO_INCREMENT PRIMARY KEY,
                             order_id CHAR(36),
                             product_id VARCHAR(50),
                             quantity INT,
                             CONSTRAINT fk_order_items_order
                                 FOREIGN KEY (order_id) REFERENCES orders(order_id)
);

CREATE TABLE idempotency_keys (
                                  id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                  idempotency_key VARCHAR(255) UNIQUE,
                                  request_hash TEXT,
                                  response TEXT,
                                  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);