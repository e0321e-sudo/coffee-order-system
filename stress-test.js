import http from 'k6/http'; // [이 줄이 빠져있을 거예요!]
import { check, sleep } from 'k6';
import { uuidv4 } from 'https://jslib.k6.io/k6-utils/1.4.0/index.js';

export default function () {
    const baseUrl = 'http://localhost:8080/api';
    const userSuffix = (__VU % 100).toString().padStart(4, '0');
    const phoneNumber = `010-1234-${userSuffix}`;

    // [중요] 모든 요청에 필요한 공통 헤더 (수지님 컨트롤러의 @RequestHeader 대응)
    const headers = {
        'Content-Type': 'application/json',
        'X-Kiosk-UUID': 'test-kiosk-001',
        'X-Kiosk-Secret': 'test-secret-123'
    };

    // 1단계: 유저 확인/생성
    http.get(`${baseUrl}/users/${phoneNumber}`, { headers });
    sleep(0.1);

    // 2단계: 포인트 충전 (이 주소가 PointController와 맞는지 꼭 확인!)
    const chargePayload = JSON.stringify({
        phoneNumber: phoneNumber,
        amount: 50000,
        idempotencyKey: `charge-${uuidv4()}`
    });
    const chargeRes = http.post(`${baseUrl}/points/charge`, chargePayload, { headers });

    check(chargeRes, { '충전 성공': (r) => r.status === 200 || r.status === 201 });
    sleep(0.2);

    // 3단계: 주문 (OrderController 규격에 맞춤)
    const orderPayload = JSON.stringify({
        phoneNumber: phoneNumber,
        menuId: 1,
        storeId: 1,
        kioskId: 1,
        quantity: 1,
        idempotencyKey: `order-${uuidv4()}`
    });

    const orderRes = http.post(`${baseUrl}/orders`, orderPayload, { headers });

    check(orderRes, { '주문 성공(200)': (r) => r.status === 200 });
}