# CLAUDE.md — 커피 주문 서비스 (project-work)

이 파일은 이 저장소에서 작업하는 모델을 위한 운영 매뉴얼이다. 여기 규칙은 일반적인 기본 동작보다 우선한다.

## 1. 프로젝트 정체

- **과제 프로젝트**: 커피 주문 서비스 백엔드. 요구사항은 5개 API — 회원가입 · 메뉴 조회 · 포인트 충전 · 주문/결제(+주문내역 실시간 전송) · 인기 메뉴(최근 7일 Top 3).
- **스택**: Spring Boot **4.1.0** / Java 17 / Gradle. MySQL(원장) + Redis(인기 메뉴 랭킹) + Kafka(주문 이벤트). 역할 분담은 [docs/tech-stack.md](docs/tech-stack.md)가 정의한다.
- **현재 상태**: 설계 문서 단계. 코드는 스켈레톤(`ProjectWorkApplication`)뿐이다. build.gradle에 MySQL 드라이버·Redis·Kafka·validation 의존성이 **아직 없다** — 구현 시작 시 추가해야 한다.

## 2. 사용자의 작업 방식 (따라야 할 것)

- **문서 먼저, 코드 나중.** 설계 결정은 docs/에 먼저 기록되고, 코드는 그 문서를 구현한다.
- **모든 소통·문서는 한국어.** 코드 식별자·커밋 메시지 제목만 영어 허용.
- **최소 조건.** 요구사항에 없는 것은 만들지 않는다. 확장 가능성은 문서에 "확장 지점"으로 적는 것까지만.
- **결정은 선택지 비교로.** 설계가 갈리면 표(장단점)로 후보를 제시하고 추천을 먼저 놓는다. 사용자가 고르면 그게 정책이다.

### 개발 워크플로 (결정됨)

`create-issue`(기반 이슈 + API별 1이슈) → `work-issue`(git-flow: develop에서 `feature/#N-슬러그`) → `commit`(`type: 한국어 설명 (#N)`) → `create-pr`(feature→develop, 릴리스는 develop→master) → `review-pr`(PR 코멘트로 게시). 각 단계의 상세 규칙은 `.claude/skills/` 해당 스킬이 SSOT다.

## 3. 문서 체계와 권위 (SSOT)

| 파일 | 권위 범위 |
|---|---|
| [docs/erd.md](docs/erd.md) | 스키마(테이블·컬럼·관계)의 SSOT |
| [docs/api-spec.md](docs/api-spec.md) | API 경로·요청/응답·상태코드의 SSOT |
| [docs/design-policy.md](docs/design-policy.md) | 비즈니스 정책·기술 선택 근거의 SSOT (결정 로그) |
| [docs/tech-stack.md](docs/tech-stack.md) | 기술별 역할 분담의 SSOT |
| [docs/code-convention.md](docs/code-convention.md) | 코드 작성 규칙의 SSOT |
| README.md | **사용자가 직접 편집하는 제출물.** 문서가 아니라 산출물이다. |

**규칙:** 스키마·API·정책 중 하나를 바꾸면 나머지 문서에서 관련 표현을 grep해서 함께 갱신하고, 갱신한 파일 목록을 보고한다. README.md는 예외 — 아래 M2 참조.

### 이미 결정된 정책 (재논의 금지)

- 3-엔티티 최소 구성(MEMBER·COFFEE·ORDER). 포인트 이력 테이블 없음, 1주문=1메뉴.
- 포인트 부족 시 `409`. 충전 금액 1 이상 정수.
- `ORDER.pay_amount`는 주문 시점 가격 스냅샷.
- 주문 생성→포인트 차감→저장은 단일 트랜잭션. 데이터 플랫폼 전송은 커밋 후, 실패해도 결제 롤백 없음.
- 인기 메뉴: Redis Sorted Set(일자별 키+TTL), 오늘 포함 최근 7 캘린더일, 동점 시 메뉴 ID 오름차순(앱 레벨 재정렬).

## 4. 약한 모델이 이 저장소에서 저지르는 실수 — 이름과 방지 규칙

**M1. 문서 표류(doc drift)** — 한 문서만 고치고 나머지를 안 고친다.
→ *규칙*: 스키마/API/정책 변경 시 docs/ 전체 + README에서 관련 키워드(컬럼명·경로·수치)를 검색해 영향 파일을 나열하고 docs/는 모두 갱신한다.

**M2. README 무단 덮어쓰기** — README를 docs/ 기준으로 "고쳐준다".
→ *규칙*: README.md는 사용자가 손으로 쓰는 제출물이다. 어긋남을 발견하면 **표로 보여주고 물어본다**. 지시 없이 수정 금지. (현재 알려진 어긋남: README ERD의 `point` vs docs의 `point_balance`+email, 인기 메뉴 "Kafka 토픽 스코어" vs docs의 "Redis Sorted Set".)

**M3. 11st 템플릿 흔적 복붙** — 이전 프로젝트(11번가 채팅) 위키에서 가져온 chatroom/BUYER/SELLER 예시를 그대로 쓴다.
→ *규칙*: 예시·코드는 반드시 이 프로젝트 도메인(member·coffee·order)으로 각색한다. role/status 컬럼은 스키마에 넣지 않는다. (회원가입 명세의 BUYER 문구는 사용자 템플릿 원문이라 명세에는 유지.)

**M4. 과잉 설계** — POINT_HISTORY, OrderItem, 로그인/토큰 인증, soft delete 등을 "미리" 추가한다.
→ *규칙*: 요구사항과 결정된 정책에 없는 테이블·필드·API·의존성 추가 금지. 필요하다고 판단되면 구현하지 말고 텍스트로 제안만 한다.

**M5. Spring Boot 3 관성** — Boot 3 기준 아티팩트명·패턴을 쓴다.
→ *규칙*: 이 프로젝트는 **Boot 4.1.0**이다. build.gradle을 먼저 읽어라. starter 이름이 다르다(예: `spring-boot-starter-webmvc`). 의존성을 추가할 때 Boot 3 이름을 복붙하지 마라.

**M6. `ORDER` 예약어 충돌** — `@Entity class Order`를 테이블명 지정 없이 만든다. ORDER는 MySQL 예약어라 DDL이 깨진다.
→ *규칙*: 주문 엔티티는 반드시 `@Table(name = "orders")`를 붙인다.

**M7. 트랜잭션 경계 붕괴** — Kafka 발행이나 Redis ZINCRBY를 `@Transactional` 안에 넣거나, 발행 실패 시 결제를 롤백한다.
→ *규칙*: 커밋 먼저, 부수효과는 나중. Redis 적재·Kafka 발행은 커밋 이후(`@TransactionalEventListener(phase = AFTER_COMMIT)` 등)에 수행하고, 실패해도 결제는 유지한다.

**M8. 동점 정렬 방치** — Redis `ZREVRANGE` 결과를 그대로 반환한다(동점 시 사전순이 됨).
→ *규칙*: 애플리케이션에서 (주문 횟수 desc, 메뉴 ID asc)로 재정렬한 뒤 반환한다.

**M9. Entity 노출·setter 사용** — 컨트롤러가 Entity를 반환하거나 `@Setter`로 상태를 바꾼다.
→ *규칙*: [docs/code-convention.md](docs/code-convention.md) 전문을 따른다. 응답은 record DTO, 생성은 정적 팩토리, 상태 변경은 도메인 메서드(`chargePoint`/`usePoint`).

**M10. 결정 재론** — 이미 정한 정책(§3)을 다시 묻거나 몰래 바꾼다.
→ *규칙*: design-policy.md가 결정 로그다. 바꿔야 할 근거가 생기면 "무엇이 왜 바뀌어야 하는지"를 보고하고 승인 후 변경한다.

## 5. 산출물별 품질 기준 (체크 가능한 형태)

### 문서(docs/*.md)
- [ ] 한국어로 작성. 문서 간 링크(`[x](y.md)`)가 실제 파일을 가리킨다.
- [ ] 테이블·컬럼명이 erd.md 표기와 글자 단위로 일치한다(`point_balance`, `pay_amount`, `ordered_at`).
- [ ] API 경로·상태코드가 api-spec.md와 일치한다.
- [ ] 외부 출처가 있으면 출처 링크를 상단에 명시한다.

### 스키마 변경
- [ ] mermaid `erDiagram`이 문법 오류 없이 렌더된다.
- [ ] 모든 컬럼에 타입 + 제약(PK/FK/unique/not null)이 있다.
- [ ] api-spec.md의 요청/응답 필드와 1:1로 대응된다(고아 필드 없음).

### 코드 (구현 단계)
- [ ] `./gradlew compileJava` 통과. `./gradlew test` 통과 — 실패하면 실패 출력과 함께 보고.
- [ ] 패키지: `global` + `domain.{member,coffee,order}`, 각 도메인에 controller/service/repository/entity/dto/exception.
- [ ] Entity: `@NoArgsConstructor(PROTECTED)` + `@AllArgsConstructor(PRIVATE)` + 정적 팩토리, `@Setter`·`@Data` 없음.
- [ ] 주문 엔티티에 `@Table(name = "orders")` 있음 (M6).
- [ ] 컨트롤러 응답에 Entity 타입이 등장하지 않음. 공통 응답 `{message, data}` 포맷.
- [ ] 포인트 차감+주문 저장이 하나의 `@Transactional` 안, Redis/Kafka는 밖 (M7).
- [ ] 각 API마다 최소: Service 핵심 로직 테스트 1 + 주요 예외 테스트 1 (given/when/then, 한국어 메서드명).

### 커밋
- [ ] 사용자가 요청했을 때만 커밋한다. README.md를 커밋에 포함하기 전에 반드시 현재 내용을 보여준다.

## 6. 불확실할 때 — 에스컬레이션 규칙

**즉시 멈추고 물어본다:**
1. 두 SSOT 문서가 충돌하는데 어느 쪽이 맞는지 문서로 판단 불가일 때 — 양쪽을 표로 제시.
2. §3의 결정된 정책을 바꿔야 구현이 가능할 때.
3. README.md를 수정해야 할 때 (항상).
4. 새 의존성/기술 추가가 필요할 때 (단, tech-stack.md에 이미 명시된 MySQL·Redis·Kafka·validation의 최초 의존성 추가는 예외 — 진행하고 보고).
5. 요구사항 해석이 2개 이상 갈리고 그 차이가 스키마나 API 형태를 바꿀 때 — 해석별 결과를 제시하고 추천 명시.

**묻지 않고 진행한다:**
- code-convention.md에 이미 답이 있는 것(네이밍, 계층 책임, DTO 형태).
- 문서 간 정합성 수정 중 docs/ 내부의 명백한 오탈자·표기 불일치(단, README 제외).
- 구현 내부 세부사항이 문서와 모순되지 않는 경우.

**질문 형식:** 선택지 2~4개 + 각 장단점 + 추천을 첫 번째에 배치. 열린 질문("어떻게 할까요?") 금지.
