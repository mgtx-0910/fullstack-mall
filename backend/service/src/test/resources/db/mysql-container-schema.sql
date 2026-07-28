CREATE TABLE mall_product (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    category_id BIGINT UNSIGNED NOT NULL,
    title VARCHAR(100) NOT NULL,
    subtitle VARCHAR(100) NOT NULL DEFAULT '',
    description VARCHAR(2000) NULL,
    status_code VARCHAR(20) NOT NULL,
    created_by BIGINT UNSIGNED NOT NULL,
    created_at DATETIME(3) NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE mall_product_sku (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    product_id BIGINT UNSIGNED NOT NULL,
    sku_code VARCHAR(64) NOT NULL,
    spec_text VARCHAR(200) NOT NULL,
    sale_price DECIMAL(12,2) NOT NULL,
    available_stock INT UNSIGNED NOT NULL DEFAULT 0,
    locked_stock INT UNSIGNED NOT NULL DEFAULT 0,
    version INT UNSIGNED NOT NULL DEFAULT 0,
    created_at DATETIME(3) NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_mall_product_sku_code (sku_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE mall_cart_item (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id BIGINT UNSIGNED NOT NULL,
    sku_id BIGINT UNSIGNED NOT NULL,
    quantity INT UNSIGNED NOT NULL,
    checked TINYINT UNSIGNED NOT NULL DEFAULT 1,
    created_at DATETIME(3) NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_mall_cart_item_user_sku (user_id, sku_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE mall_order (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    order_no VARCHAR(64) NOT NULL,
    user_id BIGINT UNSIGNED NOT NULL,
    status_code VARCHAR(30) NOT NULL,
    total_amount DECIMAL(12,2) NOT NULL,
    idempotency_key VARCHAR(64) NOT NULL,
    created_at DATETIME(3) NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    expires_at DATETIME(3) NOT NULL,
    paid_at DATETIME(3) NULL,
    cancelled_at DATETIME(3) NULL,
    closed_at DATETIME(3) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_mall_order_order_no (order_no),
    UNIQUE KEY uk_mall_order_user_idempotency (user_id, idempotency_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE mall_order_item (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    order_id BIGINT UNSIGNED NOT NULL,
    sku_id BIGINT UNSIGNED NOT NULL,
    product_id BIGINT UNSIGNED NOT NULL,
    product_title VARCHAR(100) NOT NULL,
    sku_code VARCHAR(64) NOT NULL,
    spec_text VARCHAR(200) NOT NULL,
    unit_price DECIMAL(12,2) NOT NULL,
    quantity INT UNSIGNED NOT NULL,
    line_amount DECIMAL(12,2) NOT NULL,
    created_at DATETIME(3) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_mall_order_item_order_id (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


CREATE TABLE mall_payment (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    payment_no VARCHAR(64) NOT NULL,
    order_id BIGINT UNSIGNED NOT NULL,
    user_id BIGINT UNSIGNED NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    status_code VARCHAR(20) NOT NULL,
    provider_transaction_no VARCHAR(64) NULL,
    callback_id VARCHAR(64) NULL,
    created_at DATETIME(3) NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    paid_at DATETIME(3) NULL,
    closed_at DATETIME(3) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_mall_payment_payment_no (payment_no),
    UNIQUE KEY uk_mall_payment_order_id (order_id),
    UNIQUE KEY uk_mall_payment_callback_id (callback_id),
    KEY idx_mall_payment_user_created_at (user_id, created_at),
    KEY idx_mall_payment_status_updated_at (status_code, updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
