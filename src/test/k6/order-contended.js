// 시나리오 2: 주문/결제 — 한정 포인트 경쟁 (초과 차감 없음 실증)
// 회원 1명에게 딱 N_OK건분만 충전하고 1000건을 동시에 주문한다.
// 원자적 UPDATE의 조건부(WHERE point_balance >= amount)가 정확히 N_OK건만 통과시키면
// 성공 201 = N_OK, 나머지는 409, 최종 잔액 0, 주문 수 = N_OK (초과 차감/음수 잔액 없음).
import http from 'k6/http';
import { check } from 'k6';
import { Counter } from 'k6/metrics';
import { BASE, JSON_HEADERS, signUp, charge, summarize } from './lib.js';

const COFFEE_ID = Number(__ENV.COFFEE_ID || 1);
const PRICE = Number(__ENV.PRICE || 4000);
const VUS = Number(__ENV.VUS || 1000);   // 동시 워커 수
const ITER = Number(__ENV.ITER || 1);    // 워커당 반복
const N_OK = Number(__ENV.N_OK || 100);  // 성공해야 하는 수(충전량으로 결정)

const cOk = new Counter('c_success_201');
const cConflict = new Counter('c_conflict_409');
const cOther = new Counter('c_other');

// 409(잔액 부족)는 이 시나리오의 정상 응답 → 실패로 집계하지 않도록 예상 상태 등록.
// setup의 회원가입(201)·충전(200)도 통과해야 하므로 2xx 전체 + 409를 예상으로.
http.setResponseCallback(http.expectedStatuses({ min: 200, max: 299 }, 409));

export const options = {
  scenarios: {
    burst: { executor: 'per-vu-iterations', vus: VUS, iterations: ITER, maxDuration: '180s' },
  },
  thresholds: {
    http_req_failed: ['rate==0'],                       // 409 예상 등록했으므로 진짜 실패(5xx 등)만 잡힘
    http_req_duration: ['p(95)<5000'],                  // 실측 2.2s 기준 여유
    c_success_201: [`count==${N_OK}`],                  // 원자적 UPDATE가 정확히 N_OK건만 통과
    c_conflict_409: [`count==${VUS * ITER - N_OK}`],    // 나머지는 전부 409
  },
};

export function setup() {
  const memberId = signUp(`k6-order-contended@test.local`);
  const balance = charge(memberId, N_OK * PRICE); // 딱 N_OK건분
  return { memberId, balance };
}

export default function (data) {
  const res = http.post(`${BASE}/orders`, JSON.stringify({
    memberId: data.memberId, coffeeId: COFFEE_ID,
  }), JSON_HEADERS);

  if (res.status === 201) cOk.add(1);
  else if (res.status === 409) cConflict.add(1);
  else cOther.add(1);

  check(res, { '201 또는 409': (r) => r.status === 201 || r.status === 409 });
}

export function handleSummary(data) {
  return summarize('order-contended', data);
}
