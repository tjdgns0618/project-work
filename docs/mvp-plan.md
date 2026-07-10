# MVP 실행 계획 (1인 개발 기준)

> 설계는 이미 확정됐다([erd.md](erd.md) · [api-spec.md](api-spec.md) · [design-policy.md](design-policy.md) · [tech-stack.md](tech-stack.md)). 이 문서는 **혼자서 완주하기 위한 범위·순서·단순화 결정·완료 기준**만 다룬다.

## 1. MVP 범위

### 포함 (In)
- 5개 API: 회원가입 · 메뉴 조회 · 포인트 충전 · 주문/결제 · 인기 메뉴
- MySQL 영속화(회원·커피·주문), 결제 트랜잭션
- 주문 시 Kafka `order.completed` 발행
- 인기 메뉴: Kafka Consumer → Redis Sorted Set 적재 → 조회
- 각 API 핵심 로직 + 주요 예외 테스트

### 제외 / 나중 (Out — "확장 지점"으로만 기록)
| 항목 | 이유 |
|---|---|
| 로그인·토큰 인증 | 요구사항이 "사용자 식별값 입력"이라 memberId 직접 전달로 충분 |
| POINT_HISTORY, OrderItem | 최소 조건. 잔액 컬럼·단일 메뉴로 충분 |
| 주문 취소·환불 | 요구사항 밖 |
| role/status, SELLER·CS·ADMIN | 커피 MVP 범위 밖 |
| 실제 데이터 수집 플랫폼 | Mock consumer/테스트로 수신만 검증 |
| CI, 모니터링, 메트릭, 배포 | MVP 완주 후 과제라면 불필요 |

## 2. 1인 개발 단순화 결정

혼자 하기 때문에 팀 개발과 다르게 잡는 것들:

| 영역 | 결정 |
|---|---|
| 인프라 기동 | `docker-compose`로 MySQL·Redis·Kafka를 1커맨드 로컬 기동. 통합 테스트는 Testcontainers 고려(선택) |
| 코드 리뷰 | 셀프 리뷰 — `review-pr` 스킬로 자기 PR에 `--comment` 게시(승인은 불가하므로 코멘트 확인 후 셀프 머지) |
| 브랜치 | git-flow 유지하되 리뷰어가 나 혼자이므로 셀프 머지. develop이 통합 브랜치 |
| 테스트 | 커버리지 목표 없음. Service 핵심 + 예외 흐름만. 컨트롤러는 상태코드·검증 위주 |
| 데이터 플랫폼 | Mock consumer 또는 `@KafkaListener` 테스트로 "발행됐다"만 검증 |

## 3. 개발 순서 (마일스톤)

이슈 분해와 1:1로 맞물린다. **단순→복잡, 뼈대 먼저 검증** 순서.

| M | 이슈 | 산출물 | 완료 신호 |
|---|---|---|---|
| M0 | 기반 설정 (`chore`) | 의존성(MySQL·Redis·Kafka·validation) 추가, docker-compose, application.yml | `docker-compose up` 후 앱이 3개 다 붙어 기동 |
| M1 | global + 엔티티 (`feat`) | ApiResponse, 예외 핸들러, Member/Coffee/Order 엔티티(+`@Table("orders")`) | `./gradlew compileJava` 통과 |
| M2 | 회원가입 + 메뉴 조회 (`feat`) | 가장 단순한 CRUD로 계층 뼈대 검증 | 두 API가 명세대로 응답, 테스트 통과 |
| M3 | 포인트 충전 (`feat`) | 잔액 증가, amount 검증(400), 회원 없음(404) | 충전 후 잔액 반영 확인 |
| M4 | 주문/결제 (`feat`) | **핵심.** 잔액 확인→차감→저장 단일 트랜잭션, 커밋 후 Kafka 발행 | 주문 시 잔액 차감 + `order.completed` 발행 확인, 잔액 부족 409 |
| M5 | 인기 메뉴 (`feat`) | **가장 복잡.** Kafka Consumer→Redis ZINCRBY, 7일 합산 조회, 동점 재정렬 | 주문 발생 후 인기 메뉴에 반영, 최근 7일·Top3·동점 규칙 정확 |

크리티컬 패스: **M4 → M5**. Kafka·Redis가 얽히는 뒤 두 마일스톤이 시간·난이도의 80%다.

## 4. MVP Definition of Done

전체가 끝났다고 말할 수 있는 조건 (체크 가능):

- [ ] 5개 API가 [api-spec.md](api-spec.md)의 경로·요청/응답·상태코드와 일치
- [ ] 포인트 충전→주문 시나리오에서 잔액이 정확히 차감되고, 부족 시 409
- [ ] 주문 1건 처리 시 Kafka `order.completed`가 발행되고 Mock consumer가 사용자식별값·메뉴ID·결제금액을 수신
- [ ] 최근 7일 주문 기준 인기 메뉴 Top 3가 정확(동점 시 메뉴 ID 오름차순)
- [ ] `./gradlew test` 전체 통과
- [ ] `docker-compose up`만으로 로컬 실행 가능
- [ ] README 0번(설계 내용·의도·전략·기술 선택) 완비 — *이미 충족*

## 5. 리스크 & 대응

| 리스크 | 대응 |
|---|---|
| 로컬에서 Kafka·Redis 세팅이 오래 걸림 | **M0에서 docker-compose를 가장 먼저 검증**한다. 인프라가 안 뜨면 M4·M5를 시작하지 않는다 |
| 주문 트랜잭션 경계 실수(M7) | Kafka 발행을 `@TransactionalEventListener(AFTER_COMMIT)`로. 커밋 전 발행 금지 |
| 인기 메뉴 Consumer 난이도 | Redis 직접 write로 임시 검증 후 Consumer로 전환하는 방식으로 단계 분리 가능. **단, 최종 폴백으로 직접 write를 택하면 [tech-stack.md](tech-stack.md)·[design-policy.md](design-policy.md)의 아키텍처 서술을 함께 수정**해야 함(문서 표류 방지) |
| 시간 부족 | M0~M4까지가 "결제되는 서비스"의 최소선. M5(인기 메뉴)는 마지막이므로, 정 급하면 여기서 범위를 조절하되 요구사항 4번이 빠지면 과제 미완성임을 인지 |

## 6. 착수 방법

```
create-issue  → M0~M5 이슈 7개 생성
work-issue #N → 마일스톤 순서대로 구현 (implement-api 절차 재사용)
commit / create-pr / review-pr → 각 이슈 마무리
```
워크플로 상세는 [CLAUDE.md](../CLAUDE.md) §개발 워크플로.
