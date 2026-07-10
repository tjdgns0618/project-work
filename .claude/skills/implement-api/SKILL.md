---
name: implement-api
description: docs/api-spec.md의 특정 API 하나를 코드 컨벤션대로 수직 슬라이스(Entity→Repository→Service→Controller→Test)로 구현한다. 트리거 — "N번 API 구현", "회원가입/메뉴조회/포인트충전/주문/인기메뉴 만들어", "api-spec대로 구현".
---

# implement-api — 명세 기반 API 수직 슬라이스 구현

## 목적

api-spec.md의 API 하나를 받아, 문서(erd·design-policy·code-convention)에 이미 결정된 대로 구현한다. **설계를 다시 하지 않는다** — 문서가 답이다.

## 입력

API 번호 또는 이름 (api-spec.md 기준: 1 회원가입 / 2 메뉴 조회 / 3 포인트 충전 / 4 주문·결제 / 5 인기 메뉴)

## 절차

### 0. 문서를 먼저 읽는다 (건너뛰기 금지)
- `docs/api-spec.md` 해당 섹션 — 경로·요청/응답 필드·상태코드가 계약이다.
- `docs/erd.md` — 엔티티 필드는 여기 표기 그대로.
- `docs/code-convention.md` — 패키지·네이밍·Lombok·DTO·테스트 규칙.
- `docs/design-policy.md` — 트랜잭션·집계 정책.
- `build.gradle` — **Spring Boot 4.1.0이다.** Boot 3 아티팩트명을 복붙하지 않는다.

### 1. 의존성 확인
해당 API에 필요한 의존성이 build.gradle에 없으면 추가하고 보고한다 (tech-stack.md에 명시된 범위 내에서만):
- 전 API 공통: `spring-boot-starter-validation`, MySQL 드라이버(`com.mysql:mysql-connector-j`)
- 5번 인기 메뉴: `spring-boot-starter-data-redis`
- 4번 주문: `org.springframework.kafka:spring-kafka`
그 외 의존성이 필요해 보이면 추가하지 말고 물어본다.

### 2. 패키지 구조

```
com.example.projectwork
├── global
│   ├── response   → ApiResponse<T> { message, data }
│   ├── exception  → ServiceException, GlobalExceptionHandler, 공통 ErrorCode
│   └── config     → (필요 시) RedisConfig, KafkaConfig
└── domain
    └── {member|coffee|order}
        ├── controller / service / repository / entity / dto / exception
```

global의 ApiResponse·예외 처리가 아직 없으면 첫 구현 때 함께 만든다. 이미 있으면 재사용한다 — 중복 생성 금지.

### 3. 계층별 규칙 (요약 — 전문은 code-convention.md)

- **Entity**: `@Getter` + `@NoArgsConstructor(PROTECTED)` + `@AllArgsConstructor(PRIVATE)` + 정적 팩토리 `create(...)`. `@Setter`·`@Data` 금지. 상태 변경은 도메인 메서드(`chargePoint`, `usePoint`).
- **주문 엔티티는 반드시 `@Table(name = "orders")`** — ORDER는 MySQL 예약어.
- **DTO**: Java record. Request에 Bean Validation(`@NotBlank`, `@Positive` 등). Response는 정적 `from(entity)`.
- **Controller**: 요청/응답 처리만. `ApiResponse.success(...)` 반환, 생성은 201.
- **Service**: 비즈니스 로직·존재 확인·잔액 검증. 실패는 `ServiceException(ErrorCode)`.
- **ErrorCode**: `HttpStatus` + 메시지만. 코드 문자열("GLOBAL_500" 류) 넣지 않는다.

### 4. API별 핵심 주의점

| API | 주의점 |
|---|---|
| 1 회원가입 | email unique → 중복 시 409. password는 해시 저장(BCrypt 등 — 없으면 spring-security-crypto만 추가할지 질문) |
| 2 메뉴 조회 | 실패 케이스 없음. 빈 배열 허용 |
| 3 포인트 충전 | amount 1 이상 검증은 DTO(`@Positive`), 회원 존재는 Service(404) |
| 4 주문/결제 | 잔액 확인→차감→주문 저장이 **하나의 `@Transactional`**. Redis ZINCRBY·Kafka 발행은 **커밋 이후**(`@TransactionalEventListener(phase = AFTER_COMMIT)`), 실패해도 결제 유지. 잔액 부족 409 |
| 5 인기 메뉴 | `ZUNIONSTORE`(오늘 포함 7개 일자 키) → `ZREVRANGE 0 2 WITHSCORES` → **앱에서 (횟수 desc, ID asc) 재정렬**. 일자별 키 TTL 8일 |

### 5. 테스트 (구현과 같은 턴에 작성)

우선순위와 최소 수량:
1. Service 핵심 로직 테스트 1개 이상
2. 주요 예외 시나리오 테스트 1개 이상 (예: `존재하지_않는_회원이면_예외가_발생한다`)
3. Controller 요청/검증/상태코드 테스트 (Service는 mock)

규칙: given/when/then 주석, 한국어 메서드명, 테스트당 하나의 동작, Entity 생성은 정적 팩토리.

### 6. 검증 후 보고

```
./gradlew compileJava   # 통과 필수
./gradlew test          # 통과 필수 — 실패 시 실패 출력 그대로 보고
```

보고 내용: 생성/수정 파일 목록, 테스트 결과, 명세 대비 구현 매핑(경로·상태코드·필드), 문서와 달라진 점(있다면 — 그리고 그 경우 sync-docs 실행을 제안).

## 완료 기준

- [ ] 명세의 경로·메서드·요청/응답 필드·상태코드가 코드와 1:1 일치
- [ ] compileJava·test 통과
- [ ] Entity에 setter 없음, 컨트롤러 응답에 Entity 없음
- [ ] (4번) 트랜잭션 경계 규칙 준수 / (5번) 동점 재정렬 존재
- [ ] 테스트 최소 수량 충족
