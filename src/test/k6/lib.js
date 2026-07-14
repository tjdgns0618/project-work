// k6 부하 테스트 공용 헬퍼 — 회원 생성/충전, 요약 출력.
import http from 'k6/http';

export const BASE = __ENV.BASE || 'http://localhost:8080/api/v1';
export const JSON_HEADERS = { headers: { 'Content-Type': 'application/json' } };

// 회원 가입 → memberId 반환. 부하 테스트 전 DB를 비웠다는 전제(이메일 중복 없음).
export function signUp(email) {
  const res = http.post(`${BASE}/members`, JSON.stringify({
    email, password: 'P@ssw0rd!', name: 'load',
  }), JSON_HEADERS);
  if (res.status !== 201) {
    throw new Error(`signUp 실패 status=${res.status} body=${res.body}`);
  }
  return JSON.parse(res.body).data.id;
}

// 포인트 충전(단건). 부하가 아니라 setup용.
export function charge(memberId, amount) {
  const res = http.post(`${BASE}/members/${memberId}/points`, JSON.stringify({ amount }), JSON_HEADERS);
  if (res.status !== 200) {
    throw new Error(`charge 실패 status=${res.status} body=${res.body}`);
  }
  return JSON.parse(res.body).data.pointBalance;
}

// handleSummary용 — 핵심 지표를 stdout 텍스트 + JSON 파일로 반환.
export function summarize(name, data, extra = {}) {
  const m = data.metrics;
  const num = (v) => (v === undefined ? 'n/a' : Math.round(v * 100) / 100);
  const reqs = m.http_reqs ? m.http_reqs.values.count : 0;
  const dur = m.http_req_duration ? m.http_req_duration.values : {};
  const failRate = m.http_req_failed ? m.http_req_failed.values.rate : 0;

  const lines = [
    ``,
    `================ ${name} ================`,
    `총 요청(http_reqs)      : ${reqs}`,
    `실패율(http_req_failed) : ${num(failRate * 100)} %`,
    `지연 avg / p95 / max ms : ${num(dur.avg)} / ${num(dur['p(95)'])} / ${num(dur.max)}`,
  ];
  for (const [k, v] of Object.entries(extra)) lines.push(`${k} : ${v}`);
  // 커스텀 카운터
  for (const key of Object.keys(m)) {
    if (key.startsWith('c_')) lines.push(`${key} : ${m[key].values.count}`);
  }
  // 임계치(thresholds) 결과 — 하나라도 FAIL이면 k6 exit code 1
  const thLines = [];
  for (const key of Object.keys(m)) {
    const th = m[key].thresholds;
    if (!th) continue;
    for (const [expr, res] of Object.entries(th)) {
      thLines.push(`  ${res.ok ? 'PASS ✓' : 'FAIL ✗'}  ${key} ${expr}`);
    }
  }
  if (thLines.length) lines.push(`-- thresholds --`, ...thLines);
  lines.push(`==========================================`, ``);

  const out = { stdout: lines.join('\n') };
  out[`${name}.summary.json`] = JSON.stringify(data, null, 2);
  return out;
}
