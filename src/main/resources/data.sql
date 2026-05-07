SET FOREIGN_KEY_CHECKS = 0;

-- 매장
INSERT IGNORE INTO stores (id, name, address, is_active, created_at)
VALUES (1, '수지네 커피 창원점', '창원시 의창구', true, NOW());

-- 키오스크
INSERT IGNORE INTO kiosks (id, store_id, kiosk_uuid, name, secret_key, is_active, created_at)
VALUES (1, 1, 'test-uuid-001', '메인 키오스크', 'secret123', true, NOW());

-- 카테고리
INSERT IGNORE INTO categories (id, name, display_order, is_visible, created_at)
VALUES (1, '커피', 1, true, NOW());

-- 메뉴
INSERT IGNORE INTO menus (id, category_id, name, price, is_visible, created_at)
VALUES (1, 1, '아메리카노', 4500, true, NOW());

-- 메뉴 재고
INSERT IGNORE INTO menu_stocks (id, store_id, menu_id, stock, is_sold_out)
VALUES (1, 1, 1, 11, false);

-- 사용자
INSERT IGNORE INTO users (id, phone_number, point, created_at)
VALUES (1, '01012345678', 50000, NOW());

SET FOREIGN_KEY_CHECKS = 1;