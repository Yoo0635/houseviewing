# Resume Metrics Report

## Environment
- Date:
- Git commit:
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
