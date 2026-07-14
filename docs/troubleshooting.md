# 트러블슈팅 로그

하네스·빌드·테스트 실패를 조사한 기록을 **append-only**로 누적한다. `troubleshoot` 스킬이 이 파일을 관리한다.

- **용도**: 같은 증상 재발 시 재조사 방지 + 수정 근거 이어가기.
- **권위**: 이 파일은 SSOT가 **아니다**(1회성 조사 기록). 설계 결정은 [design-policy.md](design-policy.md)가, 스키마는 [erd.md](erd.md)가, API는 [api-spec.md](api-spec.md)가 갖는다.
- **승격**: 반복적으로 재발하는 함정은 [CLAUDE.md](../CLAUDE.md) §4의 M 목록으로 옮긴다.

조사 시작 전 증상 키워드로 이 파일을 먼저 grep한다. 엔트리 형식은 `troubleshoot` 스킬의 템플릿을 따른다.

---

## TS-001 PointConcurrencyTest 데드락으로 테스트 행 — 2026-07-13
- **상태**: 해결
- **증상**: 동시성 테스트가 진행 로그 없이 멈추고 5분 타임아웃으로 종료(exit 124).
- **재현 명령**: `./gradlew test --tests "*PointConcurrencyTest"`
- **로그 요약**: 스레드 덤프상 워커 스레드들은 `start.await()`에서, 메인 스레드는 `ready.await()`에서 무한 대기. 예외·실패 메시지 없이 배리어에서 정지.
- **원인**: `ready` CountDownLatch는 `threads`(50)로 초기화됐는데 ExecutorService는 `Executors.newFixedThreadPool(16)`. 먼저 실행된 16개 태스크가 `start.await()`에서 블록돼 `ready`가 0에 도달하지 못하는 배리어 데드락(풀 크기 < 배리어 카운트).
- **수정**: 풀 생성을 `Executors.newFixedThreadPool(threads)`로 변경해 배리어 카운트와 동시 실행 스레드 수를 일치시킴. (파일: `src/test/java/com/example/projectwork/domain/member/PointConcurrencyTest.java`)
- **재검증**: `./gradlew test` → PointConcurrencyTest 포함 전체 통과(37 tests, 0 failures). 50-스레드 동시 차감에서 정확히 10건 성공·잔액 0 확인.
- **연관**: #17, PR #18

## TS-002 k6 1000 VU 동시 커넥트 시 ~70% 연결 거절 — 2026-07-14
- **상태**: 해결(원인 규명, 부하 모델 변경으로 우회)
- **증상**: k6 `VUS=1000, iterations=1`로 주문 부하 시 요청의 약 70%가 앱에 도달조차 못 하고 실패. 나머지만 정상 처리.
- **재현 명령**: `k6 run -e VUS=1000 -e ITER=1 src/test/k6/order-full.js`
- **로그 요약**: `Post http://localhost:8080/...: dial tcp 127.0.0.1:8080: connectex: No connection could be made because the target machine actively refused it.` 다수. 앱 로그엔 해당 요청 흔적 없음(도달 전 거절).
- **원인**: 1000개 소켓을 같은 순간 개설 → 커널 listen 백로그(accept 큐) 초과로 TCP 계층에서 거절. 톰캣 `server.tomcat.accept-count`를 1000으로 올려도 Windows 루프백 백로그 한계로 동일. 애플리케이션 로직·정합성과 무관(도달한 요청은 100% 정확 처리, 원장 일치).
- **수정**: 부하 모델을 **200 동시 워커 × 5회 = 1000 요청**(`per-vu-iterations`, `VUS=200 ITER=5`)으로 변경 → 모든 요청이 앱에 도달, 실패 0. 강한 동시성(200 in-flight)은 유지되어 row 잠금 경쟁은 그대로 실증.
- **재검증**: `k6 run -e VUS=200 -e ITER=5 src/test/k6/order-full.js` → 1000/1000 성공, 실패율 0%, 최종 잔액 0, 주문 1000/결제합 4,000,000 정확 일치.
- **연관**: [load-test.md](load-test.md) §5
