// 시나리오 1: 주문/결제 — 전량 성공 (동시성 정합성 실증)
// 회원 1명에게 1000×가격을 충전하고 1000건을 동시에 주문한다.
// 원자적 UPDATE가 유실/이중차감 없이 직렬화되면 1000건 전부 201, 최종 잔액 0.
import http from 'k6/http';
import { check } from 'k6';
import { Counter } from 'k6/metrics';
import { BASE, JSON_HEADERS, signUp, charge, summarize } from './lib.js';

const COFFEE_ID = Number(__ENV.COFFEE_ID || 1); // 가격 4000 가정
const PRICE = Number(__ENV.PRICE || 4000);
const VUS = Number(__ENV.VUS || 1000);  // 동시 워커 수
const ITER = Number(__ENV.ITER || 1);   // 워커당 반복
const TOTAL = VUS * ITER;               // 총 주문 시도

const cOk = new Counter('c_success_201');
const cConflict = new Counter('c_conflict_409');
const cOther = new Counter('c_other');

export const options = {
  scenarios: {
    burst: { executor: 'per-vu-iterations', vus: VUS, iterations: ITER, maxDuration: '180s' },
  },
  thresholds: {
    http_req_failed: ['rate==0'],        // 전량 성공 — 실패 0건
    http_req_duration: ['p(95)<7000'],   // 실측 3.5s의 ×2 (단일 회원 row 경쟁 최악치 여유)
  },
};

export function setup() {
  const memberId = signUp(`k6-order-full@test.local`);
  const balance = charge(memberId, TOTAL * PRICE); // 전량 성공 가능하도록 충전
  return { memberId, balance };
}

export default function (data) {
  const res = http.post(`${BASE}/orders`, JSON.stringify({
    memberId: data.memberId, coffeeId: COFFEE_ID,
  }), JSON_HEADERS);

  if (res.status === 201) cOk.add(1);
  else if (res.status === 409) cConflict.add(1);
  else cOther.add(1);

  check(res, { '201 성공': (r) => r.status === 201 });
}

export function handleSummary(data) {
  return summarize('order-full', data);
}
