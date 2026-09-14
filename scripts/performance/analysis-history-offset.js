import http from 'k6/http';
import { check } from 'k6';
import { Counter, Trend } from 'k6/metrics';

const baseUrl = __ENV.BASE_URL || 'http://127.0.0.1:18080';
const token = __ENV.ACCESS_TOKEN;
const expectedItems = Number(__ENV.EXPECTED_ITEMS);

const historyDuration = new Trend('analysis_history_duration', true);
const historyItems = new Counter('analysis_history_items');
const historyBytes = new Counter('analysis_history_response_bytes');

export const options = {
  vus: 10,
  iterations: 100,
  thresholds: {
    checks: ['rate==1'],
    http_req_failed: ['rate==0'],
  },
};

export default function () {
  const response = http.get(`${baseUrl}/analyses?offset=0`, {
    headers: { Authorization: `Bearer ${token}` },
    tags: { endpoint: 'analysis-history' },
  });
  const body = response.status === 200 ? response.json() : null;
  const items = Array.isArray(body) ? body : body?.items;

  check(response, {
    'status is 200': (res) => res.status === 200,
    'response shape is valid': () => Array.isArray(items),
    'expected history count returned': () => items?.length === expectedItems,
  });

  historyItems.add(items?.length || 0);
  historyBytes.add(response.body?.length || 0);
  historyDuration.add(response.timings.duration);
}

export function handleSummary(summary) {
  return { [__ENV.K6_SUMMARY_PATH]: JSON.stringify(summary, null, 2) };
}
