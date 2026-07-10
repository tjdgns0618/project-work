---
name: create-issue
description: docs/ 기준 문서에서 작업을 도출해 GitHub 이슈를 생성한다(gh CLI). 기반 이슈 + API별 1이슈 단위로 분해하고, 완료 조건은 검증 가능한 형태로 작성한다. 트리거 — "이슈 만들어", "이슈 생성", "N번 API 이슈", "작업 분해".
---

# create-issue — 기준 문서 기반 이슈 생성

## 목적

docs/(api-spec, erd, design-policy, tech-stack)를 근거로 GitHub 이슈를 만든다. 이슈는 나중에 work-issue 스킬이 그대로 실행할 수 있을 만큼 자기완결적이어야 한다.

## 저장소

`tjdgns0618/project-work` — gh CLI 인증 완료 상태. 이슈 생성은 외부 공개 행위이므로 **초안을 먼저 보여주고 확인받은 뒤** `gh issue create`를 실행한다.

## 이슈 분해 단위 (결정됨)

**기반 이슈 먼저, 그 다음 API별 1이슈.** 표준 분해:

| 순서 | 이슈 | 라벨 |
|---|---|---|
| 1 | 프로젝트 기반 설정 (의존성: MySQL·Redis·Kafka·validation, application.yml, docker-compose) | `chore` |
| 2 | global 패키지 + 엔티티 3종 (ApiResponse, 예외 처리, Member/Coffee/Order) | `feat` |
| 3~7 | API 1개당 이슈 1개 (회원가입 / 메뉴 조회 / 포인트 충전 / 주문·결제 / 인기 메뉴) | `feat` |

이미 만들어진 이슈가 있으면 `gh issue list`로 확인해 중복 생성하지 않는다.

## 제목 규칙

커밋 컨벤션과 동일한 타입 접두사: `feat: 포인트 충전 API 구현`, `chore: 프로젝트 기반 설정`
타입: `feat` `fix` `docs` `refactor` `test` `chore`

## 본문 템플릿

```markdown
## 배경
<!-- 어느 요구사항·문서에서 나온 작업인지. 반드시 docs 근거를 링크 -->
docs/api-spec.md의 「N. XXX」 구현. (관련: docs/design-policy.md §M, docs/erd.md)

## 작업 내용
- [ ] 세부 작업 1 (예: PointChargeRequest record + @Positive 검증)
- [ ] 세부 작업 2 (예: MemberService.chargePoint — 회원 없으면 404)
- [ ] 테스트 (Service 핵심 1 + 예외 1 이상)

## 완료 조건
<!-- 검증 가능한 문장만. "잘 동작한다" 금지 -->
- [ ] POST /api/v1/members/{id}/points 가 명세의 요청/응답/상태코드와 일치
- [ ] amount 0 이하 → 400, 회원 없음 → 404
- [ ] ./gradlew test 통과

## 참고 문서
- docs/api-spec.md #N / docs/erd.md / docs/design-policy.md 해당 절
- 컨벤션: docs/code-convention.md, CLAUDE.md
```

## 절차

1. **문서 읽기** — 대상 작업이 걸친 docs 섹션을 Read. 문서에 없는 내용을 지어내지 않는다.
2. **라벨 확보** — `gh label list`로 확인, 없으면 생성: `gh label create feat --color 0e8a16` 등 (`feat`·`fix`·`docs`·`refactor`·`test`·`chore`).
3. **초안 작성** — 위 템플릿으로 이슈별 초안을 만들어 표(제목·라벨·완료 조건 요약)로 제시하고 확인받는다.
4. **생성** — 확인 후 `gh issue create --title "..." --body "..." --label "..."`. 본문은 임시 파일로 작성해 `--body-file`로 넘긴다 (PowerShell 이스케이프 사고 방지).
5. **보고** — 생성된 이슈 번호·URL 목록.

## 규칙

- 완료 조건은 반드시 **상태코드·경로·명령어 수준**으로 검증 가능해야 한다.
- API 이슈의 작업 내용은 수직 슬라이스(Entity→Repository→Service→Controller→Test) 전체를 담는다 — 계층 쪼개기 금지.
- 이슈 하나에 API 두 개를 묶지 않는다.
- 요구사항에 없는 작업(M4 과잉 설계)을 이슈로 만들지 않는다.

## 완료 기준

- [ ] 모든 이슈에 docs 근거 링크 존재
- [ ] 모든 완료 조건이 검증 가능한 문장
- [ ] 사용자 확인 후 생성함
- [ ] 이슈 번호·URL 보고함
