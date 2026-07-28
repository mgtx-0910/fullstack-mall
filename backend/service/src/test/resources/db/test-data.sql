INSERT INTO mall_user (id, username, nickname, password_hash, role_code, status, created_at, updated_at) VALUES
    (1, 'xiaoming', '小明', '$2y$10$g8xKXITjooV38ecOr.UyNew1tX78o7Pal6ztpRutXYjZZ9gHBi6K2', 'USER', 1, TIMESTAMP '2026-07-01 09:00:00', TIMESTAMP '2026-07-01 09:00:00'),
    (2, 'xiaohong', '小红', '$2y$10$g8xKXITjooV38ecOr.UyNew1tX78o7Pal6ztpRutXYjZZ9gHBi6K2', 'USER', 1, TIMESTAMP '2026-07-02 10:00:00', TIMESTAMP '2026-07-02 10:00:00'),
    (3, 'old_user', '已停用用户', '$2y$10$g8xKXITjooV38ecOr.UyNew1tX78o7Pal6ztpRutXYjZZ9gHBi6K2', 'USER', 0, TIMESTAMP '2026-07-03 11:00:00', TIMESTAMP '2026-07-03 11:00:00'),
    (4, 'book_fan', '二手书爱好者', '$2y$10$g8xKXITjooV38ecOr.UyNew1tX78o7Pal6ztpRutXYjZZ9gHBi6K2', 'USER', 1, TIMESTAMP '2026-07-04 12:00:00', TIMESTAMP '2026-07-04 12:00:00'),
    (5, 'admin', '运营管理员', '$2y$10$3Y1LC9nQQh2FdVVG9OUSM.E8xul1/NL3yhDVGLgau4BNM8JMJ6ZdK', 'ADMIN', 1, TIMESTAMP '2026-07-05 13:00:00', TIMESTAMP '2026-07-05 13:00:00');


INSERT INTO mall_category (id, name, status, sort_order, created_at, updated_at) VALUES
    (1, '手机数码', 1, 10, TIMESTAMP '2026-07-06 09:00:00', TIMESTAMP '2026-07-06 09:00:00'),
    (2, '图书音像', 1, 20, TIMESTAMP '2026-07-06 10:00:00', TIMESTAMP '2026-07-06 10:00:00'),
    (3, '家居生活', 0, 30, TIMESTAMP '2026-07-06 11:00:00', TIMESTAMP '2026-07-06 11:00:00');

INSERT INTO mall_product (id, category_id, title, subtitle, description, status_code, created_by, created_at, updated_at) VALUES
    (1, 1, '九成新 iPhone 15', '原装配件齐全', '自用手机，功能正常，轻微使用痕迹。', 'ON_SALE', 5, TIMESTAMP '2026-07-07 09:00:00', TIMESTAMP '2026-07-07 09:00:00'),
    (2, 1, 'Nintendo Switch OLED', '白色 OLED 版本', '商品资料尚未补充完整。', 'DRAFT', 5, TIMESTAMP '2026-07-08 10:00:00', TIMESTAMP '2026-07-08 10:00:00'),
    (3, 2, 'Java 核心技术卷 I', '经典 Java 入门书', '已下架的演示商品。', 'OFF_SHELF', 5, TIMESTAMP '2026-07-09 11:00:00', TIMESTAMP '2026-07-09 11:00:00'),
    (4, 2, '深入理解计算机系统', '计算机系统经典教材', '正版二手书，少量笔记。', 'ON_SALE', 5, TIMESTAMP '2026-07-10 12:00:00', TIMESTAMP '2026-07-10 12:00:00');


INSERT INTO mall_product_sku
    (id, product_id, sku_code, spec_text, sale_price, available_stock, locked_stock, version, created_at, updated_at)
VALUES
    (1, 1, 'IPHONE15-BLACK-128G', '黑色 / 128G', 4599.00, 10, 0, 0, TIMESTAMP '2026-07-07 09:10:00', TIMESTAMP '2026-07-07 09:10:00'),
    (2, 2, 'SWITCH-OLED-WHITE', '白色 / OLED', 1699.00, 8, 0, 0, TIMESTAMP '2026-07-08 10:10:00', TIMESTAMP '2026-07-08 10:10:00'),
    (3, 3, 'JAVA-CORE-V1', '中文版 / 卷 I', 45.00, 0, 0, 0, TIMESTAMP '2026-07-09 11:10:00', TIMESTAMP '2026-07-09 11:10:00'),
    (4, 4, 'CSAPP-3E-CN', '中文版 / 第 3 版', 88.00, 5, 0, 0, TIMESTAMP '2026-07-10 12:10:00', TIMESTAMP '2026-07-10 12:10:00');


INSERT INTO mall_cart_item (id, user_id, sku_id, quantity, checked, created_at, updated_at) VALUES
    (1, 1, 1, 2, 1, TIMESTAMP '2026-07-11 09:00:00', TIMESTAMP '2026-07-11 09:00:00'),
    (2, 2, 4, 1, 0, TIMESTAMP '2026-07-11 10:00:00', TIMESTAMP '2026-07-11 10:00:00');
