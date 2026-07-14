// 시나리오 3: 포인트 충전 — 원자적 증가 누적 (Lost Update 없음 실증)
// 잔액 0 회원에게 amount를 1000건 동시 충전한다.
// 원자적 UPDATE(point_balance = point_balance + amount)면 최종 잔액 = 1000×amount.
// 경쟁은 없지만(모두 증가) 동시 읽기-수정-쓰기 유실이 있으면 최종 잔액이 부족해진다.
import http from 'k6/http';
import { check } from 'k6';
import { Counter } from 'k6/metrics';
import { BASE, JSON_HEADERS, signUp, summarize } from './lib.js';

const VUS = Number(__ENV.VUS || 1000);  // 동시 워커 수
const ITER = Number(__ENV.ITER || 1);   // 워커당 반복
const AMOUNT = Number(__ENV.AMOUNT || 100);

const cOk = new Counter('c_success_200');
const cOther = new Counter('c_other');

export const options = {
  scenarios: {
    burst: { executor: 'per-vu-iterations', vus: VUS, iterations: ITER, maxDuration: '180s' },
  },
  thresholds: {
    http_req_failed: ['rate==0'],        // 전량 성공 — 실패 0건
    http_req_duration: ['p(95)<6000'],   // 실측 3.1s의 ×2
  },
};

export function setup() {
  const memberId = signUp(`k6-charge@test.local`);
  return { memberId };
}

export default function (data) {
  const res = http.post(`${BASE}/members/${data.memberId}/points`, JSON.stringify({
    amount: AMOUNT,
  }), JSON_HEADERS);

  if (res.status === 200) cOk.add(1);
  else cOther.add(1);

  check(res, { '200 성공': (r) => r.status === 200 });
}

export function handleSummary(data) {
  return summarize('charge', data);
}
