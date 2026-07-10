# 커피 주문 서비스 API 명세서 (MVP)

- Base URL: `/api/v1`
- 공통 실패 응답: `{ "message": "에러 메시지" }`

---

## 1. 회원 가입

**Method & Path:** `POST /api/v1/members`
**설명:** 신규 회원을 등록한다.
**인증:** 불필요

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| email | string | Y | 로그인 이메일(MEMBER.email, unique) |
| password | string | Y | 평문 비밀번호(서버에서 password_hash로 저장) |
| name | string | Y | 회원 이름(MEMBER.name) |

```json
{
  "email": "buyer@11st.test",
  "password": "P@ssw0rd!",
  "name": "김구매"
}
```

**Response 성공 `201 Created`**

```json
{
  "message": "회원 가입이 완료되었습니다.",
  "data": { "id": 1, "email": "buyer@11st.test", "name": "김구매" }
}
```

**Response 실패**

| 상태 | 의미 |
|------|------|
| 400 | 필수 값 누락/형식 오류(@NotBlank) |
| 409 | 이메일 중복(MEMBER.email unique 위반) |

> 가입 시 역할은 항상 BUYER, 상태는 ACTIVE입니다. 가입 요청에서 역할을 받지 않으며 SELLER·CS_ADMIN·SUPER_ADMIN 역할 변경 API는 MVP에서 제공하지 않습니다.

---

## 2. 커피 메뉴 목록 조회

**Method & Path:** `GET /api/v1/coffees`
**설명:** 등록된 커피 메뉴(ID, 이름, 가격) 목록을 조회한다.
**인증:** 불필요

**Request** — 없음

**Response 성공 `200 OK`**

```json
{
  "message": "커피 메뉴 조회가 완료되었습니다.",
  "data": [
    { "id": 1, "name": "아메리카노", "price": 4000 },
    { "id": 2, "name": "카페라떼", "price": 4500 }
  ]
}
```

**Response 실패**

| 상태 | 의미 |
|------|------|
| — | 별도 실패 케이스 없음(항상 목록 반환, 없으면 빈 배열) |

---

## 3. 포인트 충전

**Method & Path:** `POST /api/v1/members/{memberId}/points`
**설명:** 회원의 포인트를 충전한다. (1원 = 1P)
**인증:** 불필요 (MVP — 사용자 식별값을 경로로 입력받음)

**Path Variable**

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| memberId | number | Y | 충전 대상 회원 ID(MEMBER.id) |

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| amount | number | Y | 충전 금액(1 이상 정수) |

```json
{
  "amount": 10000
}
```

**Response 성공 `200 OK`**

```json
{
  "message": "포인트 충전이 완료되었습니다.",
  "data": { "memberId": 1, "pointBalance": 10000 }
}
```

**Response 실패**

| 상태 | 의미 |
|------|------|
| 400 | amount 누락/0 이하/형식 오류 |
| 404 | 존재하지 않는 회원(memberId) |

---

## 4. 커피 주문 / 결제

**Method & Path:** `POST /api/v1/orders`
**설명:** 회원이 커피를 주문하고 포인트로 결제한다. 결제 성공 시 주문 내역을 데이터 수집 플랫폼으로 실시간 전송한다.
**인증:** 불필요 (MVP — 사용자 식별값을 본문으로 입력받음)

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| memberId | number | Y | 주문 회원 ID(MEMBER.id) |
| coffeeId | number | Y | 주문 메뉴 ID(COFFEE.id) |

```json
{
  "memberId": 1,
  "coffeeId": 2
}
```

**Response 성공 `201 Created`**

```json
{
  "message": "주문 및 결제가 완료되었습니다.",
  "data": {
    "orderId": 1,
    "memberId": 1,
    "coffeeId": 2,
    "payAmount": 4500,
    "pointBalance": 5500,
    "orderedAt": "2026-07-10T14:00:00"
  }
}
```

**Response 실패**

| 상태 | 의미 |
|------|------|
| 400 | 필수 값 누락/형식 오류 |
| 404 | 존재하지 않는 회원 또는 메뉴 |
| 409 | 포인트 잔액 부족 |

> 주문 생성 → 포인트 차감 → 주문 내역 저장은 하나의 트랜잭션으로 처리한다. 주문 이벤트는 결제 커밋 이후 Kafka(`order.completed` 토픽)로 발행하며, 이 이벤트가 데이터 수집 플랫폼 전송과 인기 메뉴 Redis 적재의 공통 소스다. 발행 실패가 결제 자체를 롤백하지는 않는다. 상세는 [tech-stack.md](tech-stack.md) 참고.

---

## 5. 인기 메뉴 목록 조회

**Method & Path:** `GET /api/v1/coffees/popular`
**설명:** 최근 7일(오늘 포함, 캘린더일 기준) 주문 횟수가 많은 커피 메뉴 상위 3개를 조회한다.
**인증:** 불필요

**Request** — 없음

**Response 성공 `200 OK`**

```json
{
  "message": "인기 메뉴 조회가 완료되었습니다.",
  "data": [
    { "rank": 1, "id": 2, "name": "카페라떼", "price": 4500, "orderCount": 120 },
    { "rank": 2, "id": 1, "name": "아메리카노", "price": 4000, "orderCount": 98 },
    { "rank": 3, "id": 5, "name": "바닐라라떼", "price": 5000, "orderCount": 98 }
  ]
}
```

**Response 실패**

| 상태 | 의미 |
|------|------|
| — | 별도 실패 케이스 없음(집계 결과가 3개 미만이면 있는 만큼만 반환, 없으면 빈 배열) |

> - 집계 기간: 오늘 포함 최근 7 캘린더일. (예: 오늘이 7/10이면 7/4 00:00 ~ 현재)
> - 동점(orderCount 동일) 시 메뉴 ID 오름차순으로 순위를 매긴다. 위 예시의 rank 2·3이 98로 동점이므로 id 1이 앞선다.
> - 집계는 Redis Sorted Set을 서빙 계층으로 사용한다. Redis는 `order.completed` Kafka Consumer가 채우며, 주문 원장(ORDER)이 정확성의 원천(source of truth)이다. 상세는 [design-policy.md](design-policy.md) 참고.
