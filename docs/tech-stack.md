# 기술 스택 & 활용 방식

> 각 기술이 어느 기능에, 어떤 방식으로 쓰이는지 정리한다.
> 관련 문서: [design-policy.md](design-policy.md), [erd.md](erd.md), [api-spec.md](api-spec.md)

## 한눈에 보기

| 기술 | 역할 | 주요 사용처 |
|------|------|-------------|
| Spring Boot | 애플리케이션 프레임워크 | 전 API (REST 계층, 비즈니스 로직, 트랜잭션) |
| MySQL | 원장 데이터 저장소 (source of truth) | 회원 · 커피 · 주문 영속화, 결제 정합성 |
| Redis | 조회 최적화 서빙 계층 | 인기 메뉴 랭킹 집계 (Kafka Consumer가 적재) |
| Kafka | 실시간 이벤트 스트리밍 (단일 이벤트 소스) | 주문 이벤트 → 데이터 수집 플랫폼 + 인기 메뉴 Redis 적재 |

---

## Spring Boot

**역할:** 서비스 전체의 기반 프레임워크.

- **REST API 계층** — `@RestController`로 5개 API(회원가입·메뉴조회·포인트충전·주문결제·인기메뉴) 제공. 경로는 `/api/v1/...` 규칙.
- **비즈니스 로직** — Service 계층에서 포인트 충전/차감, 주문 검증, 잔액 확인 처리.
- **트랜잭션 관리** — `@Transactional`로 **주문 생성 → 포인트 차감 → 주문 저장**을 원자적으로 묶는다.
- **연동 지점** — Spring Data JPA(MySQL), Spring Data Redis(Redis), Spring for Apache Kafka(Kafka Producer)를 각 계층에서 사용.
- **비밀번호 해싱** — `spring-security-crypto`의 `BCryptPasswordEncoder`로 회원 비밀번호를 해시 저장한다(#3). 보안 스타터 전체는 도입하지 않는다.

## MySQL

**역할:** 결제·정산 정합성을 책임지는 **원장(source of truth)**.

- **저장 대상** — `MEMBER`(포인트 잔액 포함), `COFFEE`, `ORDER` 테이블. ([erd.md](erd.md))
- **트랜잭션 정합성** — 포인트 차감과 주문 저장을 하나의 트랜잭션으로 커밋해 이중 결제·잔액 불일치를 방지.
- **결제금액 스냅샷** — `ORDER.pay_amount`에 주문 시점 가격 저장 → 이후 가격 변경과 무관하게 매출·주문 금액 보존.
- **인기 메뉴 정확성의 근거** — Redis 랭킹이 유실돼도 `ORDER.ordered_at` / `coffee_id`를 `GROUP BY`로 집계해 재구축 가능.

## Redis

**역할:** 인기 메뉴 조회를 위한 **랭킹 서빙 계층** (조회 성능 최적화). Kafka `order.completed` 이벤트로 채워지는 materialized view.

- **자료구조** — Sorted Set.
- **적재** — 서비스가 직접 넣지 않는다. **`order.completed` Consumer**가 이벤트를 받아 일자별 키에 카운트 누적:
  `ZINCRBY popular:coffee:{yyyyMMdd} 1 {coffeeId}`
- **멱등 적재** — Kafka at-least-once 재전달 대비, 적재 전 `SET dedup:order:{orderId} 1 NX EX {약 8일}`로 **처음 처리하는 주문일 때만** `ZINCRBY`. (중복 집계 방지, [design-policy.md](design-policy.md) §4)
- **조회** — 오늘 포함 최근 7 캘린더일 키를 합산 후 상위 조회:
  `ZUNIONSTORE`(7개 일자 키) → `ZREVRANGE ... 0 2 WITHSCORES`
- **윈도우 관리** — 일자별 키에 TTL(약 8일) → "최근 7일"이 키 구성만으로 자연스럽게 유지, 오래된 데이터 자동 만료.
- **동점 보정** — 동점 시 애플리케이션에서 (횟수 desc, 메뉴ID asc)로 재정렬.
- **정확성 원칙** — Redis는 서빙 전용, 원천은 MySQL. (상세 [design-policy.md](design-policy.md))

## Kafka

**역할:** 주문 이벤트의 **단일 소스**. 데이터 수집 플랫폼 전송(요구사항 3번)과 인기 메뉴 Redis 적재(요구사항 4번)가 모두 이 이벤트 하나에서 갈라져 나온다.

- **Producer** — 결제 **커밋 이후** 주문 이벤트를 토픽으로 1건 발행. 페이로드: 주문ID · 사용자 식별값 · 메뉴ID · 결제금액 · 주문시각. (주문ID = 인기 메뉴 멱등 키)
- **토픽(예)** — `order.completed`.
- **비동기 · 결제 비차단** — 발행 실패가 결제 트랜잭션을 롤백하지 않는다. (결제 정합성 > 전송 성공)
- **Consumer 2종** —
  - **인기 메뉴 Consumer**: 이벤트를 받아 주문ID로 멱등 확인 후 Redis에 `ZINCRBY`로 카운트 적재(중복 재전달은 무시).
  - **데이터 수집 플랫폼 Consumer**: 같은 토픽을 소비. MVP에서는 Mock consumer 또는 테스트 코드로 수신(사용자 식별값·메뉴ID·결제금액)을 검증.
- **왜 Kafka인가** — 주문 이벤트를 단일 소스로 두면 데이터 전송과 랭킹 적재를 같은 스트림에서 분기할 수 있다. 수집 플랫폼 장애가 결제 경로에 영향을 주지 않고, Redis 유실 시에도 토픽 재소비로 재구축이 가능하다.

---

## 데이터 흐름 요약 (주문 1건 기준)

```
[Client] POST /api/v1/orders
   │
   ▼
[Spring Boot Service]  ── @Transactional ──┐
   ├─ MySQL: 잔액 확인 → 포인트 차감 → ORDER insert   (원자 커밋)
   └───────────────────────────────────────┘
        │ (커밋 성공 후)
        └─ Kafka: produce → topic "order.completed"
                     │
        ┌────────────┴─────────────┐
        ▼                          ▼
[인기 메뉴 Consumer]        [데이터 수집 플랫폼 Consumer]
 Redis ZINCRBY               (Mock consumer / 테스트로 수신 검증)
 popular:coffee:{today}
```

> 커밋 이후의 Kafka 발행과 그로부터 갈라지는 Redis 적재·플랫폼 전송은 결제 성공을 전제로 하는 후속 작업이며, 실패해도 결제는 유지된다.
