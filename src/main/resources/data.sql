-- 7. 관리자 등록 (비밀번호: 1234)
INSERT INTO admins (id, email, password, refresh_token, created_at)
VALUES (1, 'admin@test.com', '$2a$10$Ux.gM3CK9vKcafKQE.XJKuTXczm4joTwH1uqtDOXCs.Go81SoS9f6', null, NOW());
-- 1. 매장 등록
INSERT INTO stores (id, name, address, is_active, created_at)
VALUES (1, '수지네 커피 창원점', '창원시 의창구', true, NOW());

-- 2. 키오스크 등록
INSERT INTO kiosks (id, store_id, kiosk_uuid, name, secret_key, is_active, created_at)
VALUES (1, 1, 'test-uuid-001', '메인 키오스크', 'secret123', true, NOW());

-- 3. 카테고리 등록
INSERT INTO categories (id, name, display_order, is_visible, created_at)
VALUES (1, '커피', 1, true, NOW());

-- 4. 메뉴 등록
INSERT INTO menus (id, category_id, name, price, is_visible, created_at)
VALUES (1, 1, '아메리카노', 4500, true, NOW());

-- 5. 메뉴 재고 등록
INSERT INTO menu_stocks (id, store_id, menu_id, stock, is_sold_out)
VALUES (1, 1, 1, 11, false);

-- 6. 사용자 등록
INSERT INTO users (id, phone_number, point, created_at)
VALUES (1, '01012345678', 50000, NOW());
