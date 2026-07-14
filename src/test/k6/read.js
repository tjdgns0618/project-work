// 시나리오 4: 조회 — 메뉴/인기메뉴 (읽기 처리량·지연 측정)
// 읽기 전용이라 동시성 정합성 검증 대상이 아니다. 검증 목표는 다르다:
//   - 안정성: 실패율 0 %, 5xx 없음 (동시 부하에서 커넥션풀/스레드풀이 버티는가)
//   - 성능: p95 지연, 초당 처리량(RPS)이 허용 범위인가
//   - 랭킹 일관성: 인기메뉴가 캐시(Redis)에서 오류 없이 응답되는가
import http from 'k6/http';
import { check } from 'k6';
import { Counter } from 'k6/metrics';
import { BASE, summarize } from './lib.js';

const VUS = Number(__ENV.VUS || 1000);  // 동시 워커 수
const ITER = Number(__ENV.ITER || 1);   // 워커당 반복

const cMenu = new Counter('c_menu_200');
const cPopular = new Counter('c_popular_200');
const cOther = new Counter('c_other');

export const options = {
  scenarios: {
    burst: { executor: 'per-vu-iterations', vus: VUS, iterations: ITER, maxDuration: '120s' },
  },
  thresholds: {
    http_req_failed: ['rate==0'],        // 조회 실패 0건
    http_req_duration: ['p(95)<500'],    // 실측 202ms의 ×2.5 (캐시 조회 관례선 500ms)
  },
};

export default function () {
  const menu = http.get(`${BASE}/coffees`);
  (menu.status === 200 ? cMenu : cOther).add(1);
  check(menu, { '메뉴 200': (r) => r.status === 200 });

  const popular = http.get(`${BASE}/coffees/popular`);
  (popular.status === 200 ? cPopular : cOther).add(1);
  check(popular, { '인기 200': (r) => r.status === 200 });
}

export function handleSummary(data) {
  return summarize('read', data);
}
