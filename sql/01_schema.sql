SET NAMES utf8mb4;

CREATE DATABASE IF NOT EXISTS fullstack_mall
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;

USE fullstack_mall;

CREATE TABLE IF NOT EXISTS mall_user (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '用户主键',
    username VARCHAR(50) NOT NULL COMMENT '登录用户名',
    nickname VARCHAR(50) NOT NULL COMMENT '用户昵称',
    password_hash VARCHAR(100) NOT NULL COMMENT 'BCrypt 密码哈希，绝不保存明文密码',
    role_code VARCHAR(20) NOT NULL DEFAULT 'USER' COMMENT '角色：USER-普通用户，ADMIN-运营管理员',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-禁用，1-正常',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
        ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_mall_user_username (username),
    KEY idx_mall_user_status_created_at (status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商城用户表';

CREATE TABLE IF NOT EXISTS mall_category (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '分类主键',
    name VARCHAR(50) NOT NULL COMMENT '分类名称',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '排序值，越小越靠前',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
        ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_mall_category_name (name),
    KEY idx_mall_category_status_sort (status, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商品分类表';

CREATE TABLE IF NOT EXISTS mall_product (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '商品主键',
    category_id BIGINT UNSIGNED NOT NULL COMMENT '分类 ID',
    title VARCHAR(100) NOT NULL COMMENT '商品标题',
    subtitle VARCHAR(100) NOT NULL DEFAULT '' COMMENT '商品副标题',
    description VARCHAR(2000) NULL COMMENT '商品描述',
    status_code VARCHAR(20) NOT NULL DEFAULT 'DRAFT' COMMENT '状态：DRAFT、ON_SALE、OFF_SHELF',
    created_by BIGINT UNSIGNED NOT NULL COMMENT '创建管理员 ID',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
        ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_mall_product_category_id (category_id),
    KEY idx_mall_product_status_created_at (status_code, created_at),
    KEY idx_mall_product_created_by (created_by)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='SPU 商品表';


CREATE TABLE IF NOT EXISTS mall_product_sku (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'SKU 主键',
    product_id BIGINT UNSIGNED NOT NULL COMMENT '所属 SPU 商品 ID',
    sku_code VARCHAR(64) NOT NULL COMMENT '全局唯一 SKU 编码',
    spec_text VARCHAR(200) NOT NULL COMMENT '规格描述，例如 黑色 / 128G',
    sale_price DECIMAL(12,2) NOT NULL COMMENT '销售价',
    available_stock INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '可售库存',
    locked_stock INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '锁定库存，阶段 7 使用',
    version INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
        ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_mall_product_sku_code (sku_code),
    KEY idx_mall_product_sku_product_id (product_id),
    CONSTRAINT chk_mall_product_sku_price CHECK (sale_price > 0),
    CONSTRAINT chk_mall_product_sku_available_stock CHECK (available_stock >= 0),
    CONSTRAINT chk_mall_product_sku_locked_stock CHECK (locked_stock >= 0),
    CONSTRAINT chk_mall_product_sku_version CHECK (version >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商品 SKU 表';


CREATE TABLE IF NOT EXISTS mall_cart_item (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '购物车行 ID',
    user_id BIGINT UNSIGNED NOT NULL COMMENT '所属用户 ID',
    sku_id BIGINT UNSIGNED NOT NULL COMMENT 'SKU ID',
    quantity INT UNSIGNED NOT NULL COMMENT '购买数量，当前限制 1 到 99',
    checked TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '是否勾选：0 否，1 是',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
        ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_mall_cart_item_user_sku (user_id, sku_id),
    KEY idx_mall_cart_item_user_updated_at (user_id, updated_at),
    CONSTRAINT chk_mall_cart_item_quantity CHECK (quantity BETWEEN 1 AND 99),
    CONSTRAINT chk_mall_cart_item_checked CHECK (checked IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='购物车明细表';

CREATE TABLE IF NOT EXISTS mall_order (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '订单主键',
    order_no VARCHAR(64) NOT NULL COMMENT '对外订单号',
    user_id BIGINT UNSIGNED NOT NULL COMMENT '下单用户 ID',
    status_code VARCHAR(30) NOT NULL COMMENT '订单状态：PENDING_PAYMENT、PAID、CANCELLED、CLOSED',
    total_amount DECIMAL(12,2) NOT NULL COMMENT '订单成交总金额',
    idempotency_key VARCHAR(64) NOT NULL COMMENT '客户端幂等键，同一用户内唯一',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
        ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    expires_at DATETIME(3) NOT NULL COMMENT '支付截止时间',
    paid_at DATETIME(3) NULL COMMENT '支付成功时间',
    cancelled_at DATETIME(3) NULL COMMENT '用户取消时间',
    closed_at DATETIME(3) NULL COMMENT '系统超时关闭时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_mall_order_order_no (order_no),
    UNIQUE KEY uk_mall_order_user_idempotency (user_id, idempotency_key),
    KEY idx_mall_order_user_created_at (user_id, created_at),
    KEY idx_mall_order_status_created_at (status_code, created_at),
    KEY idx_mall_order_status_expires_at (status_code, expires_at),
    CONSTRAINT chk_mall_order_total_amount CHECK (total_amount >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='订单主表';

CREATE TABLE IF NOT EXISTS mall_order_item (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '订单项主键',
    order_id BIGINT UNSIGNED NOT NULL COMMENT '订单 ID',
    sku_id BIGINT UNSIGNED NOT NULL COMMENT '下单时 SKU ID',
    product_id BIGINT UNSIGNED NOT NULL COMMENT '下单时商品 ID',
    product_title VARCHAR(100) NOT NULL COMMENT '下单时商品标题快照',
    sku_code VARCHAR(64) NOT NULL COMMENT '下单时 SKU 编码快照',
    spec_text VARCHAR(200) NOT NULL COMMENT '下单时规格快照',
    unit_price DECIMAL(12,2) NOT NULL COMMENT '下单时单价快照',
    quantity INT UNSIGNED NOT NULL COMMENT '购买数量',
    line_amount DECIMAL(12,2) NOT NULL COMMENT '订单行金额',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_mall_order_item_order_id (order_id),
    KEY idx_mall_order_item_sku_id (sku_id),
    CONSTRAINT chk_mall_order_item_unit_price CHECK (unit_price > 0),
    CONSTRAINT chk_mall_order_item_quantity CHECK (quantity > 0),
    CONSTRAINT chk_mall_order_item_line_amount CHECK (line_amount > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='订单项快照表';



CREATE TABLE IF NOT EXISTS mall_payment (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '支付流水主键',
    payment_no VARCHAR(64) NOT NULL COMMENT '本系统支付流水号',
    order_id BIGINT UNSIGNED NOT NULL COMMENT '订单 ID，一张订单一条支付流水',
    user_id BIGINT UNSIGNED NOT NULL COMMENT '支付用户 ID',
    amount DECIMAL(12,2) NOT NULL COMMENT '来自订单快照的支付金额',
    status_code VARCHAR(20) NOT NULL COMMENT '支付状态：PENDING、SUCCESS、CLOSED',
    provider_transaction_no VARCHAR(64) NULL COMMENT '模拟支付平台交易号',
    callback_id VARCHAR(64) NULL COMMENT '回调事件唯一 ID',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
        ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    paid_at DATETIME(3) NULL COMMENT '支付成功时间',
    closed_at DATETIME(3) NULL COMMENT '支付关闭时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_mall_payment_payment_no (payment_no),
    UNIQUE KEY uk_mall_payment_order_id (order_id),
    UNIQUE KEY uk_mall_payment_callback_id (callback_id),
    KEY idx_mall_payment_user_created_at (user_id, created_at),
    KEY idx_mall_payment_status_updated_at (status_code, updated_at),
    CONSTRAINT chk_mall_payment_amount CHECK (amount >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='支付流水表';
