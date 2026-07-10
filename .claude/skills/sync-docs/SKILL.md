---
name: sync-docs
description: docs/ 5개 문서와 README.md 사이의 정합성(스키마·API·정책·기술 표현)을 검사하고 어긋남을 표로 보고한 뒤 docs/만 수정한다. 트리거 — "문서 정합성", "문서 싱크", "docs 확인", "문서 어긋남", 스키마/API/정책을 변경한 직후.
---

# sync-docs — 문서 정합성 검사·동기화

## 목적

이 프로젝트는 문서 먼저(docs-first)로 움직인다. 문서가 서로 어긋나면 그 어긋남이 그대로 코드 버그와 과제 감점이 된다. 이 스킬은 6개 파일의 정합성을 축(axis) 단위로 대조한다.

## 대상 파일

1. `docs/erd.md` — 스키마 SSOT
2. `docs/api-spec.md` — API SSOT
3. `docs/design-policy.md` — 정책 SSOT
4. `docs/tech-stack.md` — 기술 역할 SSOT
5. `docs/code-convention.md` — 코드 규칙 SSOT
6. `README.md` — 사용자 제출물 (**읽기만, 수정 금지**)

## 절차

### 1. 전부 읽는다
6개 파일을 모두 Read한다. 일부만 읽고 판단하지 않는다.

### 2. 검사 축별로 대조한다

| 축 | 대조 내용 | 기준(SSOT) |
|---|---|---|
| 테이블·컬럼명 | `point_balance`, `pay_amount`, `ordered_at`, `email`, `password_hash` 등 컬럼 표기가 모든 문서에서 동일한가 | erd.md |
| API 경로·메서드 | `/api/v1/members`, `/api/v1/coffees`, `/api/v1/coffees/popular`, `/api/v1/orders` 등이 문서마다 같은가 | api-spec.md |
| 상태코드 | 잔액 부족 409, 회원 없음 404 등 실패 코드가 문서 간 일치하는가 | api-spec.md |
| 정책 수치 | 7 캘린더일, Top 3, 1원=1P, 충전 1 이상, 동점 시 ID 오름차순 | design-policy.md |
| 기술 선택 | 인기 메뉴=Redis Sorted Set, 주문 전송=Kafka, 원장=MySQL이 모든 문서에서 같은 역할로 서술되는가 | tech-stack.md |
| 링크 | 문서 간 `[x](y.md)` 링크가 실제 존재하는 파일을 가리키는가 | — |
| mermaid | erDiagram 블록이 문법 오류 없이 렌더 가능한가 | — |

### 3. 결과를 표로 보고한다

어긋남마다 한 줄:

| # | 축 | 파일 A (표기) | 파일 B (표기) | SSOT 기준 정답 | 조치 |
|---|---|---|---|---|---|

### 4. 수정한다 — 단, 범위 준수

- **docs/ 내부 어긋남**: SSOT 기준으로 즉시 수정하고 수정 파일 목록을 보고한다.
- **README.md와의 어긋남**: 절대 직접 수정하지 않는다. 표로 보여주고 "README를 docs 기준으로 맞출까요, docs를 README 기준으로 바꿀까요?"를 선택지로 묻는다. README가 맞고 docs가 틀린 경우(사용자가 마음을 바꾼 경우)도 있다.
- **SSOT끼리 충돌하고 어느 쪽이 맞는지 문서로 판단 불가**: 수정하지 말고 양쪽을 제시하고 묻는다.

## 알려진 상습 어긋남 (우선 확인)

- README ERD의 `point` vs docs의 `point_balance` + `email`/`password_hash`
- README 설계 의도의 "Kafka 토픽 메뉴 스코어로 인기 메뉴 조회" vs design-policy의 "Redis Sorted Set" — 사용자의 의도가 "Kafka 이벤트 → consumer가 Redis 적재"일 수 있으므로 반드시 해석을 묻는다
- design-policy·api-spec의 "전송은 Mock으로 처리" vs tech-stack의 "Kafka Producer 발행" — Mock의 위치(전송부인가 수신부인가) 표현 통일 여부

## 완료 기준

- [ ] 6개 파일 전부 읽음
- [ ] 검사 축 7개 전부 대조함
- [ ] 어긋남 0건이면 "정합성 이상 없음"과 검사 축 목록 보고
- [ ] docs/ 수정분은 파일 목록으로 보고, README 관련은 질문으로 종료
