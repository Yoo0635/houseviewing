# Resume Metrics Report

## Environment
- Date: 2026-08-03
- Git commit: `de5c198` + local changes
- Spring profile: `performance`
- Database: MySQL 8.0
- Redis: Redis 7
- Hikari maximum pool size: 10

## Signup Concurrency
- Command: `BASE_URL=http://localhost:8080 RUN_ID=<id> sh scripts/performance/run-signup-load.sh`
- Input: same email and login ID, 100 VU, 100 total requests
- Created:
- Conflict:
- Unexpected:
- Persisted users with target login ID:
- Hikari pending connections observed:

Resume wording:
- 동시 회원가입 요청 100건을 Hikari 최대 커넥션 10개 조건에서 반복 검증하고, DB Unique 제약과 도메인 예외 변환으로 중복 저장 0건을 확인

## PDF Failure And Retry
- 503 scenario command:
- Timeout scenario command:
- Initial analysis responses:
- Retry queued jobs:
- Retry success after Python recovery:
- Duplicate reports:
- Dead-letter jobs:

Resume wording:
- PDF 생성 서버의 503·타임아웃 장애를 분석 트랜잭션과 분리하고 Redis 큐 기반 재처리로 전환해, PDF 장애 시에도 분석 결과 응답과 후속 재처리를 검증

## Coverage
- Command: `sh scripts/performance/run-jacoco-scope.sh`
- Scope: diagnosis query services, analysis services, report services, Python client
- Baseline line coverage:
- After line coverage:
- Baseline branch coverage:
- After branch coverage:
- Added unit tests:

Resume wording:
- 진단 워크플로우 핵심 클래스 범위를 고정한 JaCoCo 측정에서 단위 테스트를 추가해 커버리지 변화를 XML 기준으로 검증

## Registry Change Idempotency
- Command: `BASE_URL=http://localhost:18080 STUB_URL=http://localhost:18081 REQUESTS=100 SUMMARY_PATH=scripts/performance/results/change-diagnosis-idempotency-current-seeded.json sh scripts/performance/run-change-diagnosis-load.sh`
- Input: same user, same house, same fixed registry snapshot, 100 VU, 100 total requests
- Redis lock key: `diff-diagnosis:lock:<houseId>:<snapshotHash>`
- DB uniqueness: `(house_id, analysis_type, snapshot_hash)`
- Created: 1
- Conflict: 99
- Unexpected: 0
- DIFF analyses persisted: 1
- PDF reports persisted: 1
- External diff analysis calls: 1
- External diff PDF calls: 1

Resume wording:
- 동일 등기 변동 요청 100건을 k6로 재현하고, 스냅샷 해시 기반 멱등키·Redis 분산락·DB Unique 제약을 적용해 분석 및 PDF 외부 호출을 각각 1회로 제한하고 중복 저장·생성 0건을 검증

## Kakao Delay And Hikari Pool
- Baseline command: `BASE_URL=http://host.docker.internal:18080 REQUESTS=100 MONITOR_DURATION=20s sh scripts/performance/run-kakao-pool-delay.sh`
- After command: `BASE_URL=http://localhost:18080 REQUESTS=100 MONITOR_DURATION=20s SUMMARY_PATH=scripts/performance/results/kakao-pool-delay-current-rerun.json sh scripts/performance/run-kakao-pool-delay.sh`
- Input: house register 100 VU, Kakao address stub fixed delay 1s, Hikari maximum pool size 10
- Baseline house created: not measured in this run
- Baseline house failed: not measured in this run
- Baseline Hikari pending max: not measured in this run
- After house created: 100
- After house failed: 0
- After Hikari pending max: 0
- Kakao stub calls: 101 total, including 1 setup request and 100 load requests

Resume wording:
- 카카오 주소 API에 1초 지연을 주입한 100건 동시 부하에서 외부 호출을 DB 트랜잭션 밖으로 분리해, 최대 풀 10개 조건에서도 주택 등록 100건 성공·실패 0건·HikariCP 대기 최대 0건을 검증

## Run Blockers
- 2026-08-03: k6 was installed with Homebrew and script syntax was verified.
- 2026-08-03: Local MySQL/Redis Docker containers, Spring Boot performance profile, and external API stub were started successfully.
- 2026-08-03: Baseline-before-refactor numbers were not measured in this run; current values are verified after-change numbers only.
