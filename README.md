# ☕ Coffee Order System

> 프랜차이즈 카페의 키오스크 주문 시스템을 목표로 한 대용량 트래픽 커피 주문 서버입니다.
> 다수의 서버 인스턴스에서도 안정적으로 동작하도록 설계했습니다.

---

## 📌 프로젝트 소개

고객은 키오스크에서 전화번호를 입력해 포인트를 충전하고 커피를 주문합니다.
장바구니에 여러 메뉴를 담아 한 번에 결제하거나, 단건으로 바로 주문할 수 있습니다.
관리자는 백오피스를 통해 메뉴, 재고, 임시 휴무를 관리하며 재고 부족 알림을 실시간으로 받습니다.

### 핵심 목표

| 목표 | 설명 |
|------|------|
| 대용량 트래픽 | 다수의 서버 인스턴스에서도 안정적으로 동작 |
| 동시성 제어 | 여러 키오스크에서 동시에 주문해도 포인트/재고 정합성 보장 |
| 실시간성 | 주문 데이터 실시간 전송, 재고 부족 즉시 알림 |
| 확장성 | 카테고리/메뉴/휴무를 배포 없이 운영 가능한 구조 |

### 사용자 유형

| 유형 | 접근 방식 | 주요 기능 |
|------|----------|----------|
| 고객 | 키오스크 (UUID + Secret Key 인증) | 메뉴 조회, 포인트 충전, 장바구니, 주문/결제, 주문 취소 |
| 관리자 | 백오피스 (JWT 인증) | 메뉴 관리, 재고 관리, 임시 휴무 등록, 실시간 알림 |

---

## 🛠️ 기술 스택

| 기술 | 버전 | 용도 |
|------|------|------|
| Java | 17 | 메인 언어 |
| Spring Boot | 3.3.5 | 메인 프레임워크 |
| Spring Security + JWT | - | 관리자 인증 |
| Spring Data JPA | - | ORM |
| MySQL | 8.0 | 운영 DB |
| Redis | 7.2 | 인기 메뉴 캐싱, 장바구니, Idempotency Key |
| Apache Kafka | 3.7.0 | 주문 이벤트 전송, 재고 알림 |
| SSE | - | 백오피스 실시간 알림 |
| Docker Compose | - | 로컬 인프라 구성 |
| JUnit5 + Mockito | - | 테스트 |

---

## 🏗️ 시스템 구조

```
[ Clients ]             [ Spring Boot App ]            [ Infrastructure ]

┌─────────┐
│ 키오스크1 ├─┐
└─────────┘ │          ┌───────────────────┐          ┌──────────────┐
┌─────────┐ │ HTTP/REST│                   │ JPA/JDBC │              │
│ 키오스크2 ├─┼─────────>│  Spring Boot 서버  ├─────────>│  MySQL 8.0   │
└─────────┘ │  (UUID)  │ (다중 인스턴스 환경)   │(비관적 락)│              │
┌─────────┐ │          │                   │          └──────────────┘
│ 키오스크3 ├─┘          └─────────┬─────────┘                 ▲
└─────────┘                        │                           │ Lettuce
                                   │                   ┌───────┴──────┐
┌─────────┐    JWT Auth            │ Kafka Client      │              │
│ 백오피스  ├───────────────────────┘                   │  Redis 7.2   │
└─────────┘                        │                   │              │
                                   │                   └──────────────┘
                                   ▼
                         ┌───────────────────┐
                         │   Kafka Cluster   │
                         │ Brokers x3, KRaft │
                         └───────────────────┘
```

---

## 📦 패키지 구조

```
src/main/java/com/coffee/order/
├── common/
│   ├── aop/           # 서비스 로깅, 관리자 접근 로깅
│   ├── config/        # Security, JWT, Redis, Kafka 설정
│   ├── exception/     # BusinessException, ErrorCode
│   ├── filter/        # JwtFilter
│   ├── handler/       # GlobalExceptionHandler
│   ├── init/          # DataInitializer
│   ├── interceptor/   # KioskAuthInterceptor
│   ├── response/      # ApiResponse
│   ├── service/       # IdempotencyService
│   └── sse/           # SseEmitterManager
│
└── domain/
    ├── admin/         # 관리자 로그인, Refresh Token
    ├── cart/          # Redis 장바구니
    ├── kiosk/         # 키오스크 등록/인증
    ├── menu/          # 메뉴 CRUD, 인기 메뉴 캐싱, 스케줄러
    ├── order/         # 주문/취소, Kafka Producer/Consumer
    ├── stock/         # 재고 이력, Kafka Producer/Consumer
    ├── store/         # 매장, 임시 휴무
    └── user/          # 유저, 포인트 충전
```

---

## 🚀 실행 방법

### 사전 요구사항

- Java 17
- Docker Desktop

### 1. 인프라 실행

```bash
docker-compose up -d
```

| 서비스 | 포트 | 설명 |
|--------|------|------|
| MySQL | 3307 | 운영 DB |
| Kafka Broker 1 | 9092 | Kafka 클러스터 |
| Kafka Broker 2 | 9093 | Kafka 클러스터 |
| Kafka Broker 3 | 9094 | Kafka 클러스터 |
| Kafka UI | 8088 | Kafka 모니터링 |
| Redis | 6379 | 캐싱 / 장바구니 / Idempotency |

### 2. 서버 실행

```bash
./gradlew bootRun
```

서버 시작 시 자동으로 관리자 계정이 생성됩니다.

```
email: admin@test.com
password: 1234
```

### 3. 테스트 실행

```bash
./gradlew test
```

---

## 📋 API 명세

### 공통 응답 형식

```json
// 성공
{ "success": true, "data": { }, "message": null }

// 실패
{ "success": false, "data": null, "message": "에러 메시지" }
```

### 키오스크 API (X-Kiosk-UUID, X-Kiosk-Secret 헤더 필요)

| Method | URL | 설명 |
|--------|-----|------|
| GET | `/api/users/{phoneNumber}` | 유저 조회 (없으면 신규 생성, isNewUser 플래그 반환) |
| GET | `/api/menus` | 메뉴 목록 조회 (storeId, categoryId 파라미터 선택) |
| GET | `/api/menus/{menuId}` | 메뉴 상세 조회 |
| GET | `/api/menus/popular` | 인기 메뉴 TOP 3 조회 (Redis 캐싱) |
| POST | `/api/points/charge` | 포인트 충전 |
| POST | `/api/orders` | 단건 주문/결제 |
| PATCH | `/api/orders/{orderId}/cancel` | 주문 취소 (5분 이내) |
| POST | `/api/cart/items` | 장바구니 담기 (X-Kiosk-UUID 헤더로 식별) |
| GET | `/api/cart` | 장바구니 조회 |
| DELETE | `/api/cart/items/{menuId}` | 장바구니 메뉴 삭제 |
| DELETE | `/api/cart` | 장바구니 전체 비우기 |
| POST | `/api/cart/checkout` | 장바구니 전체 결제 |

### 관리자 API (Authorization: Bearer {token} 헤더 필요)

| Method | URL | 설명 |
|--------|-----|------|
| POST | `/api/admin/auth/login` | 관리자 로그인 |
| POST | `/api/admin/auth/refresh` | Access Token 재발급 |
| POST | `/api/admin/auth/logout` | 로그아웃 |
| POST | `/api/admin/stores` | 매장 등록 |
| GET | `/api/admin/stores` | 매장 목록 조회 |
| GET | `/api/admin/stores/{id}` | 매장 상세 조회 |
| PUT | `/api/admin/stores/{id}` | 매장 수정 |
| DELETE | `/api/admin/stores/{id}` | 매장 삭제 |
| POST | `/api/admin/stores/{storeId}/kiosks` | 키오스크 등록 (UUID + Secret Key 자동 발급) |
| GET | `/api/admin/stores/{storeId}/kiosks` | 키오스크 목록 조회 |
| DELETE | `/api/admin/stores/{storeId}/kiosks/{kioskId}` | 키오스크 삭제 |
| POST | `/api/admin/stores/{storeId}/special-closes` | 임시 휴무 등록 |
| GET | `/api/admin/stores/{storeId}/special-closes` | 임시 휴무 목록 조회 |
| DELETE | `/api/admin/stores/{storeId}/special-closes/{id}` | 임시 휴무 삭제 |
| POST | `/api/admin/categories` | 카테고리 등록 |
| GET | `/api/admin/categories` | 카테고리 목록 조회 |
| PUT | `/api/admin/categories/{id}` | 카테고리 수정 |
| PATCH | `/api/admin/categories/{id}/hide` | 카테고리 숨김 처리 |
| DELETE | `/api/admin/categories/{id}` | 카테고리 삭제 |
| POST | `/api/admin/menus` | 메뉴 등록 |
| GET | `/api/admin/menus` | 메뉴 목록 조회 |
| PUT | `/api/admin/menus/{id}` | 메뉴 수정 |
| PATCH | `/api/admin/menus/{id}/hide` | 메뉴 숨김 처리 |
| POST | `/api/admin/menus/{menuId}/stocks` | 재고 추가 |
| GET | `/api/admin/stock-histories` | 재고 이력 조회 (menuId, storeId 파라미터) |
| GET | `/api/admin/notifications/stream` | SSE 실시간 알림 구독 |

---

## 🔑 설계 의도 및 기술적 결정

### 1. 서버 분리가 아닌 URL 분리를 선택한 이유

| 단계 | 구조 | 이유 |
|------|------|------|
| 초기 (현재) | URL 분리 `/api/**` vs `/api/admin/**` | 빠른 개발, 단순한 운영 |
| 성장기 | 서버 분리 | 독립 배포, 독립 스케일링 |
| 대규모 | MSA + API Gateway | 팀별 독립 개발 |

> "현재 규모에선 URL 분리로 충분하고, 트래픽이 커지면 admin 서버를 별도로 분리할 수 있는 구조로 설계했습니다."

### 2. 비관적 락을 선택하고 분산 락을 쓰지 않은 이유

이 프로젝트는 **단일 DB를 공유하는 다중 서버 구조**입니다.

```
서버 1 ─┐
서버 2 ─┼──→ 공유 MySQL ← 비관적 락이 DB 레벨에서 동작
서버 3 ─┘
```

비관적 락은 DB 레벨에서 걸리기 때문에 서버가 몇 개든 자연스럽게 동시성이 보장됩니다.
분산 락(Redisson)은 MSA로 확장해서 DB가 분리되거나, Redis 캐시처럼 DB 밖의 자원을 제어할 때 도입을 고려하겠습니다.

### 3. 장바구니를 Redis로 구현한 이유

장바구니는 주문 확정 전 임시 데이터로, DB에 저장하면 미완성 데이터가 쌓이는 문제가 있습니다.
Redis Hash 구조로 저장하고 TTL 30분을 적용해 30분 동안 아무 동작 없으면 자동으로 삭제됩니다.

```
cart:{kioskUuid} → Hash { menuId: quantity, ... }
TTL: 30분 (담기/조회 시 초기화)
```

장바구니 전체 결제 시 `OrderService`를 재사용해 메뉴별로 개별 주문을 생성하며,
중간에 하나라도 실패(품절, 포인트 부족 등)하면 전체 롤백됩니다.

### 4. 커스텀 어노테이션 AOP 대신 SecurityConfig 경로 기반 인가를 선택한 이유

현재 시스템은 관리자와 키오스크 두 가지 역할만 존재하고, 관리자는 `/api/admin/**` 경로로 명확히 분리되어 있어 SecurityConfig 경로 기반으로 충분하다고 판단했습니다.
만약 SUPER_ADMIN, STORE_ADMIN처럼 권한이 세분화된다면 커스텀 어노테이션 AOP로 확장할 수 있는 구조입니다.

### 5. 중복 주문 방지 2중 전략

```
1차 방어 — Idempotency Key (Redis, TTL 5분)
  └── 같은 키로 5분 내 재요청 시 즉시 차단

2차 방어 — DB Unique 제약
  └── (userId, menuId, kioskId, order_second) 복합 Unique 제약
  └── 같은 초 내 동일 조합 주문 원천 차단
```

> Idempotency Key는 Stripe, PayPal 등 글로벌 결제 API 표준 방식입니다.

### 6. ErrorCode를 한 곳에서 관리하는 이유

현재 규모에서는 한 곳에서 전체 에러코드를 파악하는 게 유지보수에 유리하다고 판단했습니다. 도메인이 늘어나면 도메인별로 분리할 수 있는 구조입니다.

### 7. SSE를 선택하고 WebSocket을 쓰지 않은 이유

| 항목 | SSE | WebSocket |
|------|-----|-----------|
| 방향 | 서버 → 클라이언트 단방향 | 양방향 |
| 구현 복잡도 | 낮음 | 높음 |
| 알림 용도 적합성 | ✅ 적합 | 오버스펙 |

단순 재고 알림은 서버에서 클라이언트로 단방향 전송만 필요하므로 SSE가 적합합니다.

---

## ⚡ 주요 기능 흐름

### 단건 주문 흐름

```
주문 요청
  ├── Idempotency Key 중복 확인 (Redis)
  ├── 영업시간 체크 (09:00 ~ 22:30)
  ├── 임시 휴무 체크
  ├── 메뉴 품절 체크
  ├── 포인트 차감 (비관적 락)
  ├── 재고 차감 (비관적 락)
  ├── 주문 생성
  ├── 재고 이력 기록
  └── Kafka 이벤트 발행
      ├── order-completed → 데이터 수집 플랫폼 전송 (Mock)
      └── stock-alert (재고 10개 이하 시) → SSE 실시간 알림
```

### 장바구니 결제 흐름

```
장바구니 전체 결제 요청
  ├── 장바구니 조회 (Redis)
  ├── 비어있으면 에러
  └── 메뉴별 개별 주문 처리 (OrderService 재사용)
      ├── 중간 실패 시 전체 롤백 (@Transactional)
      └── 전체 성공 시 장바구니 비우기
```

### 인기 메뉴 캐싱 흐름

```
GET /api/menus/popular 요청
  ├── Redis 캐시 확인
  │   ├── 캐시 히트 → 즉시 반환 (5~20ms)
  │   └── 캐시 미스 → DB 집계 후 캐시 저장 (50~200ms)
  └── 매일 자정 스케줄러로 캐시 갱신
```

### 실시간 재고 알림 흐름

```
재고 10개 이하 감지
  └── Kafka stock-alert 발행
      └── StockConsumer 수신
          └── SSE로 백오피스 실시간 알림 🔔
              "[강남점] 아메리카노 재고 부족 (잔여 8개)"
```

<svg width="680" height="420" viewBox="0 0 680 420" xmlns="http://www.w3.org/2000/svg">
  <style>
    text { font-family: Arial, sans-serif; }
    .th { font-size: 14px; font-weight: 600; fill: #1a1a1a; }
    .ts { font-size: 12px; fill: #444; }
    .notif-title { font-size: 13px; font-weight: 600; fill: #993C1D; }
    .notif-body { font-size: 12px; fill: #993C1D; }
  </style>

  <rect width="680" height="420" fill="#ffffff"/>

  <rect x="20" y="30" width="130" height="44" rx="8" fill="#EEEDFE" stroke="#534AB7" stroke-width="1"/>
  <text class="th" x="85" y="57" text-anchor="middle" dominant-baseline="central">주문 서비스</text>

  <rect x="230" y="30" width="110" height="44" rx="8" fill="#FAEEDA" stroke="#854F0B" stroke-width="1"/>
  <text class="th" x="285" y="57" text-anchor="middle" dominant-baseline="central">Kafka</text>

  <rect x="420" y="30" width="240" height="44" rx="8" fill="#FAECE7" stroke="#993C1D" stroke-width="1"/>
  <text class="th" x="540" y="57" text-anchor="middle" dominant-baseline="central">StockConsumer / 백오피스</text>

  <line x1="85" y1="74" x2="85" y2="340" stroke="#ccc" stroke-width="1" stroke-dasharray="4 4"/>
  <line x1="285" y1="74" x2="285" y2="340" stroke="#ccc" stroke-width="1" stroke-dasharray="4 4"/>
  <line x1="540" y1="74" x2="540" y2="340" stroke="#ccc" stroke-width="1" stroke-dasharray="4 4"/>

  <rect x="30" y="100" width="110" height="52" rx="6" fill="#E6F1FB" stroke="#185FA5" stroke-width="1"/>
  <text class="ts" x="85" y="118" text-anchor="middle" fill="#185FA5">① 재고 감소</text>
  <text class="ts" x="85" y="136" text-anchor="middle" font-weight="bold" fill="#185FA5">(10개 이하 감지)</text>

  <line x1="145" y1="126" x2="270" y2="126" stroke="#BA7517" stroke-width="1.5" marker-end="url(#arrow-amber)"/>
  <text class="ts" x="207" y="118" text-anchor="middle" fill="#BA7517">② stock-alert 발행</text>

  <line x1="300" y1="180" x2="525" y2="180" stroke="#993C1D" stroke-width="1.5" marker-end="url(#arrow-coral)"/>
  <text class="ts" x="412" y="172" text-anchor="middle" fill="#993C1D">③ Consumer 이벤트 수신</text>

  <line x1="540" y1="180" x2="540" y2="240" /> <rect x="493" y="210" width="94" height="52" rx="6" fill="#FAECE7" stroke="#993C1D" stroke-width="1"/>
  <text class="ts" x="540" y="228" text-anchor="middle" fill="#993C1D">④ SSE 알림</text>
  <text class="ts" x="540" y="246" text-anchor="middle" fill="#993C1D">전송</text>

  <rect x="140" y="320" width="400" height="60" rx="8" fill="#FAECE7" stroke="#993C1D" stroke-width="1.5"/>
  <text x="340" y="342" text-anchor="middle" class="notif-title">🔔 백오피스 실시간 알림</text>
  <text x="340" y="362" text-anchor="middle" class="notif-body">"[강남점] 아메리카노 재고 부족 (잔여 8개)"</text>

  <defs>
    <marker id="arrow-amber" viewBox="0 0 10 10" refX="8" refY="5" markerWidth="6" markerHeight="6" orient="auto-start-reverse">
      <path d="M2 1L8 5L2 9" fill="none" stroke="#BA7517" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
    </marker>
    <marker id="arrow-coral" viewBox="0 0 10 10" refX="8" refY="5" markerWidth="6" markerHeight="6" orient="auto-start-reverse">
      <path d="M2 1L8 5L2 9" fill="none" stroke="#993C1D" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
    </marker>
  </defs>
</svg>


---

## 🧪 테스트

### 테스트 현황

| 테스트 클래스 | 테스트 수 | 주요 내용 |
|-------------|---------|---------|
| OrderServiceTest | 26개 | 영업시간, 임시휴무, 메뉴/재고/포인트 경계값, 취소 5분 경계값 |
| MenuStockTest | 13개 | 품절 검증, 차감/증가, 경계값, 동시성 |
| UserTest | 8개 | 충전/차감 성공·실패, 경계값, 동시성 |
| AdminServiceTest | 12개 | login/refresh/logout 성공·실패 |
| OrderTest | 10개 | 상태 전이, validateCancelable 경계값 |
| KioskAuthInterceptorTest | 7개 | UUID/SecretKey 헤더 누락·불일치 |
| MenuAdminServiceTest | 16개 | CRUD, 재고 추가, Kafka 이벤트 |
| StoreServiceTest | 13개 | CRUD 성공·실패 |
| KioskAdminServiceTest | 13개 | 등록, UUID 고유성, 삭제 |
| IdempotencyServiceTest | - | 중복 요청 감지, 타입별 독립성 |
| MenuServiceTest | - | 캐시 히트/미스, 캐시 삭제 |
| CartServiceTest | - | 장바구니 담기/조회/삭제/결제 |
| PopularMenuSchedulerTest | - | 스케줄러 동작 |
| **합계** | **160개+** | |

### 테스트 실행 환경

- H2 인메모리 DB (MySQL과 격리)
- Redis, Kafka Mock 처리
- `@Profile("test")` 적용

---

## 📊 향후 개선 계획

| 항목 | 내용 |
|------|------|
| 키오스크 프론트엔드 | 피그마 디자인 후 React/Next.js로 구현 |
| 분산 락 | MSA 확장 시 Redisson 도입 |
| Dead Letter Queue | Kafka Consumer 실패 처리 |
| Refresh Token 블랙리스트 | Redis 기반 토큰 무효화 |
| Rate Limiting | Bucket4j + Redis 기반 API 남용 방지 |