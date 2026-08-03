import http from 'k6/http';
import { check } from 'k6';
import { Counter } from 'k6/metrics';

const baseUrl = __ENV.BASE_URL || 'http://host.docker.internal:18080';
const stubUrl = __ENV.STUB_URL || 'http://host.docker.internal:18081';
const requests = Number(__ENV.REQUESTS || 100);

export const options = {
  scenarios: {
    same_change: {
      executor: 'per-vu-iterations',
      vus: requests,
      iterations: 1,
      maxDuration: '30s',
      exec: 'diagnoseSameChange',
    },
  },
};

const diagnosisCreated = new Counter('diagnosis_created');
const diagnosisConflict = new Counter('diagnosis_conflict');
const diagnosisFailed = new Counter('diagnosis_failed');

export function setup() {
  http.post(`${stubUrl}/reset`, null, { timeout: '2s' });
  const suffix = `${Date.now()}-${Math.floor(Math.random() * 100000)}`;
  const loginId = `diff-idem-${suffix}`;
  const password = 'password123';

  const registerRes = http.post(`${baseUrl}/users/register`, JSON.stringify({
    name: 'diff-idempotency',
    email: `${loginId}@example.com`,
    loginId,
    password,
  }), {
    headers: { 'Content-Type': 'application/json' },
  });
  check(registerRes, { 'user registered': (res) => res.status === 201 });

  const loginRes = http.post(`${baseUrl}/auth/login`, JSON.stringify({
    loginId,
    password,
  }), {
    headers: {
      'Content-Type': 'application/json',
      'X-Device-Id': `k6-${suffix}`,
    },
  });
  check(loginRes, { 'user logged in': (res) => res.status === 200 });
  const token = loginRes.json('accessToken');

  const houseRes = http.post(`${baseUrl}/houses/register`, JSON.stringify({
    nickname: 'diff-idempotency-house',
    originAddress: '서울특별시 강남구 테헤란로 427',
  }), {
    headers: authHeaders(token),
  });
  check(houseRes, { 'house registered': (res) => res.status === 201 });
  const houseId = houseRes.json('houseId');

  const contractRes = http.post(`${baseUrl}/contracts/register`, JSON.stringify({
    houseId,
    contractType: 'JEONSE',
    deposit: 100000000,
    monthlyAmount: 0,
    maintenanceFee: 100000,
    moveDate: '2026-08-03',
    confirmDate: '2026-08-03',
  }), {
    headers: authHeaders(token),
  });
  check(contractRes, { 'contract registered': (res) => res.status === 201 });

  const baselineRes = http.post(`${baseUrl}/performance/houses/${houseId}/baseline-analysis`, null, {
    headers: { Authorization: `Bearer ${token}` },
  });
  check(baselineRes, { 'baseline analysis seeded': (res) => res.status === 201 });

  return { token, houseId };
}

export function diagnoseSameChange(data) {
  const res = http.post(
    `${baseUrl}/performance/houses/${data.houseId}/change-diagnoses/fixed-snapshot`,
    null,
    {
      headers: { Authorization: `Bearer ${data.token}` },
      timeout: '20s',
    }
  );

  if (res.status === 201) {
    diagnosisCreated.add(1);
  } else if (res.status === 409) {
    diagnosisConflict.add(1);
  } else {
    diagnosisFailed.add(1);
  }
  check(res, {
    'created or conflict': (r) => r.status === 201 || r.status === 409,
  });
}

export function handleSummary(summary) {
  const output = __ENV.K6_SUMMARY_PATH || 'scripts/performance/results/change-diagnosis-idempotency-summary.json';
  return {
    [output]: JSON.stringify(summary, null, 2),
    stdout: textSummary(summary),
  };
}

function authHeaders(token) {
  return {
    'Content-Type': 'application/json',
    Authorization: `Bearer ${token}`,
  };
}

function metricCount(summary, name) {
  const metric = summary.metrics[name];
  return metric && metric.values ? metric.values.count : 0;
}

function textSummary(summary) {
  return JSON.stringify({
    diagnosis_created: metricCount(summary, 'diagnosis_created'),
    diagnosis_conflict: metricCount(summary, 'diagnosis_conflict'),
    diagnosis_failed: metricCount(summary, 'diagnosis_failed'),
  }, null, 2);
}
