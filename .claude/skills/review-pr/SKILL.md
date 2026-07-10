---
name: review-pr
description: PR의 diff를 code-convention.md와 CLAUDE.md 실수 목록(M1~M10) 기준으로 검사하고, 심각도별 결과를 PR 코멘트로 게시한다. 트리거 — "PR 리뷰", "리뷰해", "코드 리뷰 #N".
---

# review-pr — 기준 문서 기반 코드 리뷰

## 목적

PR을 이 프로젝트의 SSOT(docs/) 기준으로 리뷰하고 결과를 **PR 코멘트로 게시**한다(결정됨). 일반적인 코드 취향이 아니라 **이 저장소의 문서화된 규칙 위반**을 잡는 것이 우선이다.

## 입력

PR 번호. 생략 시 현재 브랜치의 PR (`gh pr view --json number`).

## 절차

### 1. PR 파악
```
gh pr view {N}                 # 본문·연결 이슈
gh pr diff {N}                 # 전체 diff
gh issue view {연결된 이슈}     # 완료 조건
```
diff만으로 판단이 어려운 파일은 브랜치를 체크아웃해 전체 파일을 읽는다.

### 2. 검사 — 3개 렌즈 순서대로

**렌즈 A: 명세 일치 (docs가 정답)**
- API 경로·메서드·요청/응답 필드·상태코드가 docs/api-spec.md와 일치하는가
- 정책 준수: 잔액 부족 409, 충전 1 이상, 7 캘린더일, 동점 ID 오름차순 (docs/design-policy.md)
- 이슈의 완료 조건이 실제로 충족되는가

**렌즈 B: 알려진 실수 (CLAUDE.md M1~M10 중 코드 관련)**
- M6: 주문 엔티티 `@Table(name = "orders")` 있는가
- M7: Kafka 발행·Redis 적재가 `@Transactional` 밖(커밋 후)인가, 발행 실패가 결제를 롤백하지 않는가
- M8: 인기 메뉴 동점 시 앱 레벨 재정렬 있는가
- M9: Entity에 `@Setter`/`@Data` 없는가, 컨트롤러가 Entity를 반환하지 않는가
- M4: 이슈 스코프 밖 코드·요구사항에 없는 기능이 섞여 있지 않은가

**렌즈 C: 컨벤션 (docs/code-convention.md)**
- 패키지 구조(global + 도메인), 클래스 네이밍, DTO record, 정적 팩토리, ErrorCode 형식(HttpStatus+메시지, 코드 문자열 없음), 테스트 규칙(given/when/then, 한국어 메서드명, Entity는 팩토리로 생성)

### 3. 로컬 검증
```
gh pr checkout {N}
./gradlew test
```
테스트 실패는 무조건 🔴.

### 4. 심각도 분류

| 등급 | 기준 | 예 |
|---|---|---|
| 🔴 Blocker | 버그, 명세 위반, M6·M7 위반, 테스트 실패 | 잔액 검증 없이 차감, ORDER 예약어 충돌 |
| 🟡 Convention | code-convention 위반 (동작은 함) | @Setter 사용, ErrorCode에 코드 문자열 |
| 🔵 Suggestion | 개선 제안 (선택) | 네이밍 개선, 테스트 케이스 추가 제안 |

### 5. PR 코멘트 게시

리뷰 본문을 스크래치패드 파일로 작성 후:
```
gh pr review {N} --comment --body-file {파일}
```
> 자기 PR은 approve 불가하므로 항상 `--comment`를 쓴다.

**코멘트 형식:**
```markdown
## 코드 리뷰 (기준: docs/code-convention.md, CLAUDE.md, api-spec.md)

### 결과 요약
🔴 N건 · 🟡 N건 · 🔵 N건 / 테스트: 통과 X개

| # | 등급 | 파일:라인 | 내용 | 근거 |
|---|---|---|---|---|
| 1 | 🔴 | OrderService.java:42 | Kafka 발행이 @Transactional 내부 | CLAUDE.md M7 |

### 상세
(항목별로 문제 코드 인용 + 수정 방향)
```
근거 열에는 반드시 문서·규칙 이름을 적는다 — 근거 없는 지적은 🔵로 강등하거나 뺀다.

### 6. 보고
게시한 코멘트 URL + 🔴 유무에 따른 권고 (🔴 있으면 "머지 전 수정 필요", 없으면 "머지 가능").

## 규칙

- 문서에 근거 없는 개인 취향 지적 금지. 애매하면 🔵.
- diff에 없는 기존 코드 문제는 리뷰 항목이 아니라 별도 언급("스코프 밖 발견")으로 분리.
- 리뷰만 한다 — 코드를 직접 고치지 않는다. 수정은 사용자가 요청하면 work-issue 흐름으로.

## 완료 기준

- [ ] 3개 렌즈 전부 검사함
- [ ] 로컬에서 테스트 실행함
- [ ] 모든 지적에 근거(문서·규칙) 명시
- [ ] PR 코멘트 게시 + URL 보고
