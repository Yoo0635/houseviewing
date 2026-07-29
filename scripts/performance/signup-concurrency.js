import http from 'k6/http';
import { check } from 'k6';
import { Counter } from 'k6/metrics';

export const options = {
  scenarios: {
    concurrent_signup: {
      executor: 'shared-iterations',
      vus: Number(__ENV.VUS || 100),
      iterations: Number(__ENV.ITERATIONS || 100),
      maxDuration: __ENV.MAX_DURATION || '30s',
    },
  },
};

const created = new Counter('signup_created');
const conflict = new Counter('signup_conflict');
const unexpected = new Counter('signup_unexpected');

export default function () {
  const baseUrl = __ENV.BASE_URL || 'http://localhost:8080';
  const runId = __ENV.RUN_ID || `${Date.now()}`;
  const payload = JSON.stringify({
    name: 'load-test-user',
    email: `signup-${runId}@example.com`,
    loginId: `signup-${runId}`,
    password: 'password123',
  });

  const response = http.post(`${baseUrl}/users/register`, payload, {
    headers: { 'Content-Type': 'application/json' },
  });

  if (response.status === 201) {
    created.add(1);
  } else if (response.status === 409) {
    conflict.add(1);
  } else {
    unexpected.add(1);
  }

  check(response, {
    'created or conflict': (res) => res.status === 201 || res.status === 409,
  });
}

export function handleSummary(data) {
  const resultPath = __ENV.RESULT_PATH || 'signup-summary.json';
  return {
    [resultPath]: JSON.stringify(data, null, 2),
    stdout: JSON.stringify({
      created: data.metrics.signup_created?.values?.count || 0,
      conflict: data.metrics.signup_conflict?.values?.count || 0,
      unexpected: data.metrics.signup_unexpected?.values?.count || 0,
      http_req_failed: data.metrics.http_req_failed?.values?.rate || 0,
    }, null, 2),
  };
}
