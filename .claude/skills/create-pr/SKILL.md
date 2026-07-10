---
name: create-pr
description: 현재 feature 브랜치를 푸시하고 develop 대상 PR을 템플릿에 맞춰 생성한다(gh CLI). 릴리스 시엔 develop→master PR. 트리거 — "PR 만들어", "PR 생성", "풀리퀘".
---

# create-pr — PR 생성

## 대상 (결정됨 — git-flow)

- **feature/#N-... → develop** (기본)
- **develop → master** (릴리스 PR — 사용자가 명시할 때만)

## 제목 규칙

커밋과 동일: `feat: 포인트 충전 API 구현 (#5)`

## 본문 템플릿

```markdown
## 요약
<!-- 무엇을 왜 — 1~3문장. 이슈 배경 반복 말고 이 PR이 한 일 -->

## 변경 사항
- <!-- 주요 변경을 계층/파일 그룹 단위로 -->

## 테스트
- [ ] `./gradlew test` 통과
```
{실제 테스트 실행 결과 요약 붙여넣기 — 통과 개수}
```

## 관련 이슈
Closes #{N}

## 셀프 체크리스트
- [ ] docs/api-spec.md 명세(경로·필드·상태코드)와 일치
- [ ] Entity에 @Setter 없음, 컨트롤러 응답에 Entity 없음
- [ ] 주문 관련이면: @Table(name = "orders"), 트랜잭션 경계(커밋 후 Kafka/Redis) 준수
- [ ] docs와 어긋나는 변경 없음 (있었다면 sync-docs 실행함)
- [ ] 이슈 스코프 밖 변경 없음
```

## 절차

### 1. 사전 검증 (생성 전 필수)
```
git status                    # 커밋 안 된 변경 없어야 함
./gradlew test                # 통과해야 함 — 결과를 본문에 첨부
git log develop..HEAD --oneline   # 이 PR에 실릴 커밋 확인
```
develop과 diff를 훑어 이슈 스코프 밖 파일이 없는지 확인한다. 있으면 PR을 만들지 말고 보고.

### 2. 셀프 체크리스트 실제 수행
템플릿의 체크 항목을 코드 대조로 하나씩 확인한다. 확인 못 한 항목을 체크된 상태로 넣지 않는다.

### 3. 푸시 & 생성
```
git push -u origin {브랜치}
gh pr create --base develop --title "..." --body-file {임시파일}
```
본문은 스크래치패드에 파일로 작성해 `--body-file`로 전달한다.

### 4. 보고
PR URL, 대상 브랜치, 포함 커밋 수, 테스트 결과. **review-pr 스킬 실행을 제안**한다.

## 릴리스 PR (develop → master)

- 제목: `release: {포함 기능 요약}`
- 본문: 포함된 이슈/PR 목록 (`Closes` 대신 목록 링크 — 이슈는 feature PR에서 이미 닫힘)
- 사전 조건: develop에서 `./gradlew test` 통과 + submit-check 스킬 통과

## 규칙

- 테스트 실패 상태로 PR을 만들지 않는다.
- PR 하나 = 이슈 하나. 여러 이슈를 한 PR에 묶지 않는다.
- Draft가 필요한 경우(미완성 공유)는 `--draft`를 쓰고 본문에 남은 작업을 명시한다.

## 완료 기준

- [ ] base가 develop (릴리스면 master)
- [ ] 본문에 실제 테스트 결과 포함
- [ ] Closes #N 연결
- [ ] 셀프 체크리스트 전 항목 실제 확인 후 체크
- [ ] PR URL 보고 + 리뷰 제안
