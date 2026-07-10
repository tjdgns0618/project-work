---
name: work-issue
description: GitHub 이슈 번호를 받아 git-flow 브랜치를 만들고 이슈의 작업 내용을 구현·검증·커밋한다. API 구현 이슈면 implement-api 스킬 절차를 따른다. 트리거 — "N번 이슈 작업", "이슈 구현", "#N 진행해".
---

# work-issue — 이슈 기반 구현

## 목적

이슈 하나를 받아 브랜치 생성부터 완료 조건 검증·커밋까지 수행한다. 이슈가 계약이다 — 이슈에 없는 작업은 하지 않는다.

## 브랜치 전략 (결정됨 — git-flow)

```
master ← develop ← feature/#{이슈번호}-{영문-슬러그}
```

- feature 브랜치는 **develop에서** 딴다. PR도 develop으로.
- **develop이 없으면** (최초 실행): `git checkout -b develop master && git push -u origin develop` 후 진행.
- 슬러그는 이슈 제목의 핵심을 영문 케밥케이스로: `feature/#5-point-charge`

## 절차

### 1. 이슈 읽기
```
gh issue view {N}
```
배경·작업 내용·완료 조건·참고 문서를 파악한다. 완료 조건이 검증 불가능하게 쓰여 있으면 작업 전에 지적하고 해석을 확인받는다.

### 2. 브랜치 준비
```
git checkout develop && git pull
git checkout -b feature/#{N}-{slug}
```
작업 트리가 더럽면(uncommitted changes) 먼저 보고하고 처리 방향을 묻는다.

### 3. 구현
- **API 구현 이슈** → `implement-api` 스킬의 절차(문서 선독 → 의존성 → 계층별 규칙 → API별 주의점 → 테스트)를 그대로 따른다.
- **기반 설정 이슈** → 이슈의 작업 내용 체크박스 순서대로. docs/tech-stack.md에 명시된 범위만.
- 공통: CLAUDE.md M1~M10 준수. 특히 M4(이슈에 없는 작업 금지), M6(`@Table(name = "orders")`), M7(트랜잭션 경계).

### 4. 완료 조건 검증 (건너뛰기 금지)
이슈의 **완료 조건 체크박스를 하나씩 실제로 실행해** 확인한다:
```
./gradlew compileJava && ./gradlew test
```
API 명세 일치는 코드를 명세와 대조. 통과 못 한 조건이 있으면 커밋하지 않고 보고한다.

### 5. 커밋 & 푸시
- `commit` 스킬 규칙으로 커밋 (타입 영어 + 한국어 설명 + `(#N)`).
- `git push -u origin feature/#{N}-{slug}`
- 이슈의 작업 내용 체크박스를 갱신: `gh issue edit {N} --body-file ...` (완료 항목 체크).

### 6. 보고 & 다음 단계
- 완료 조건 검증 결과 표 (조건 → 통과/실패 → 근거).
- create-pr 스킬 실행을 제안한다 (자동 실행하지 않음 — PR 생성은 별도 확인).

## 규칙

- **스코프 = 이슈.** 구현 중 발견한 다른 문제는 고치지 말고 보고만 한다 (새 이슈 후보로).
- 구현이 docs와 충돌하면 멈추고 묻는다 (docs가 SSOT).
- README.md는 절대 수정하지 않는다.
- 테스트 실패 상태로 푸시하지 않는다.

## 완료 기준

- [ ] feature 브랜치가 develop 기준으로 생성됨
- [ ] 이슈의 완료 조건 전부 실제 검증됨 (통과 못 하면 커밋 안 함)
- [ ] 커밋 메시지에 이슈 번호 포함
- [ ] 검증 결과 표 보고 + PR 생성 제안
