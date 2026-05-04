-- 1. 매장 등록 (isActive -> is_active)
INSERT INTO stores (id, name, address, is_active, created_at)
VALUES (1, '수지네 커피 창원점', '창원시 의창구', true, NOW());

-- 2. 키오스크 등록 (isActive -> is_active)
INSERT INTO kiosks (id, store_id, kiosk_uuid, name, secret_key, is_active, created_at)
VALUES (1, 1, 'test-uuid-001', '메인 키오스크', 'secret123', true, NOW());

-- 3. 메뉴 등록 (categoryId 필수, isVisible -> is_visible)
INSERT INTO menus (id, category_id, name, price, is_visible, created_at)
VALUES (1, 1, '아메리카노', 4500, true, NOW());

-- 4. 메뉴 재고 등록 (1번 매장의 1번 메뉴 재고를 100개로 설정)
-- 필드명은 엔티티를 확인해야겠지만, 로그상 store_id, menu_id, stock, is_sold_out이 보입니다.
INSERT INTO menu_stocks (id, store_id, menu_id, stock, is_sold_out)
VALUES (1, 1, 1, 100, false);

-- 5. 사용자 등록 (포스트맨에서 테스트할 번호와 포인트 50,000점)
INSERT INTO users (id, phone_number, point, created_at)
VALUES (1, '01012345678', 50000, NOW());