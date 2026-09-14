# 진단 이력 OFFSET 조회 성능 검증

이력 조회 SQL 실행 1,502회 → 2회(99.87% 감소), 페이징 적용한 최초 조회 1,871.20ms → 22.51ms(98.80% 감소, p95 기준)

## 검증 조건

- 기준선: 비페이징 커밋 `08fcb4e`
- 개선본: 최신 `main` 재배치 후 OFFSET 구현 커밋 `4b8ffa5`
- 데이터: 사전 진단 500건, 사후 진단 500건, 보고서 900건
- 상세 JSON: 진단 이력마다 약 10KB
- 실행 환경: 동일한 로컬 Spring Boot, 전용 MySQL 8 데이터베이스, Redis 7
- 워밍업: 각 서버 10회
- k6: 동시 사용자 10명, 총 100회 요청
- SQL 수: MySQL general log에서 한 번의 `/analyses?offset=0` 요청 중 진단·집·보고서 테이블을 조회한 SQL만 집계

## 결과

| 검증 항목 | 비페이징 | OFFSET | 변화 |
| --- | ---: | ---: | ---: |
| 이력 조회 SQL | 1,502회 | 2회 | 99.87% 감소 |
| 최초 조회 항목 | 1,000건 | 10건 | 99.00% 감소 |
| 최초 조회 중앙값 | 1,572.49ms | 15.66ms | 99.00% 감소 |
| 최초 조회 p95 | 1,871.20ms | 22.51ms | 98.80% 감소 |
| 요청당 응답 크기 | 168,335B | 2,250B | 98.66% 감소 |
| 처리량 | 6.14회/s | 582.41회/s | 94.85배 증가 |

두 실행 모두 HTTP 실패 0건, check 성공률 100%였다. 비페이징 응답은 요청마다 1,000건을 반환했고, OFFSET 응답은 `offset=0`에서 최신 10건과 다음 offset만 반환했다.

## 재현

```sh
docker exec -i mysql-server mysql -uroot -p1234 analysis_history_benchmark \
  < scripts/performance/seed-analysis-history-offset.sql

LABEL=baseline EXPECTED_ITEMS=1000 \
  scripts/performance/run-analysis-history-offset.sh

LABEL=offset EXPECTED_ITEMS=10 \
  scripts/performance/run-analysis-history-offset.sh
```

- k6 시나리오: `scripts/performance/analysis-history-offset.js`
- 기준선 원본 결과: `scripts/performance/results/baseline.json`
- OFFSET 원본 결과: `scripts/performance/results/offset.json`

SQL 집계에서는 인증 과정의 사용자·구독 조회를 제외하고 `post_analyses`, `pre_analyses`, `houses`, `post_reports`, `pre_reports` 관련 SELECT만 같은 조건으로 계산했다.
