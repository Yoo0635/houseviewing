# 등기 멱등성 및 커넥션 풀 검증 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 동일 등기 변동의 중복 분석·PDF 생성을 막고, 카카오 API 지연이 DB 커넥션 대기로 전파되지 않도록 분리한 뒤 k6로 수치를 검증한다.

**Architecture:** 등기 변동은 스냅샷 해시 기반 Redis 락과 DB Unique 제약을 조합한다. 카카오 호출은 비트랜잭션 오케스트레이터에서 수행하고 DB 저장만 별도 트랜잭션으로 제한한다.

**Tech Stack:** Spring Boot 3.5, Java 17, JPA, MySQL 8, Redis 7, WebClient, HikariCP, k6

## Global Constraints

- Hikari 최대 커넥션은 10개로 고정한다.
- 변경 전후는 같은 VU, 요청 수, 외부 API 지연 시간으로 측정한다.
- 원본 결과가 없는 수치는 이력서에 사용하지 않는다.

---

### Task 1: 카카오 지연 baseline 측정 도구

**Files:**
- Create: `scripts/performance/external-api-stub.py`
- Create: `scripts/performance/kakao-pool-delay.js`
- Create: `scripts/performance/run-kakao-pool-delay.sh`

**Interfaces:**
- Consumes: `POST /houses/register`, `GET /actuator/metrics/hikaricp.connections.pending`
- Produces: k6 요약과 Hikari pending 최댓값

- [ ] 지연 가능한 카카오 주소 응답 서버와 k6 스크립트를 작성한다.
- [ ] 현재 코드에서 Hikari 최대 10, 동일 지연·VU 조건으로 baseline을 저장한다.
- [ ] 결과 JSON에 요청 성공 수, 실패 수, pending 최댓값을 기록한다.

### Task 2: 등기 변동 멱등성

**Files:**
- Modify: `backend/src/main/java/com/house/houseviewing/domain/analysis/postanalysis/entity/PostAnalysisEntity.java`
- Modify: `backend/src/main/java/com/house/houseviewing/domain/analysis/postanalysis/repository/PostAnalysisRepository.java`
- Create: `backend/src/main/java/com/house/houseviewing/api/query/service/DiffDiagnosisLockService.java`
- Modify: `backend/src/main/java/com/house/houseviewing/api/query/service/DiffAnalysisQueryService.java`
- Test: `backend/src/test/java/com/house/houseviewing/api/query/DiffAnalysisQueryServiceTest.java`
- Test: `backend/src/test/java/com/house/houseviewing/api/query/DiffDiagnosisLockServiceTest.java`

**Interfaces:**
- Consumes: `houseId`, 등기 스냅샷 문자열
- Produces: SHA-256 멱등키, 단일 실행, 기존 결과 재사용 또는 처리 중 충돌

- [ ] 동시 요청에서 최초 요청만 실행되는 실패 테스트를 작성하고 실패를 확인한다.
- [ ] Redis `SETNX`와 소유자 토큰 비교 삭제를 구현한다.
- [ ] DIFF 분석에 snapshot hash를 저장하고 DB Unique 제약을 추가한다.
- [ ] 대상 테스트를 실행해 통과를 확인한다.

### Task 3: 카카오 호출 트랜잭션 분리

**Files:**
- Create: `backend/src/main/java/com/house/houseviewing/domain/house/service/HousePersistenceService.java`
- Modify: `backend/src/main/java/com/house/houseviewing/domain/house/service/HouseService.java`
- Modify: `backend/src/main/java/com/house/houseviewing/domain/analysis/preanalysis/service/PreAnalysisService.java`
- Modify: `backend/src/main/java/com/house/houseviewing/global/config/WebClientConfig.java`
- Test: `backend/src/test/java/com/house/houseviewing/domain/house/HouseServiceTest.java`

**Interfaces:**
- Consumes: 카카오에서 변환한 `Address`
- Produces: 외부 호출 이후 시작되는 짧은 DB 저장 트랜잭션

- [ ] 외부 호출 시 활성 트랜잭션이 없음을 검증하는 실패 테스트를 작성한다.
- [ ] DB 조회·저장을 별도 트랜잭션 빈으로 이동한다.
- [ ] WebClient 연결·응답 타임아웃과 예외 변환을 추가한다.
- [ ] 대상 테스트를 실행해 통과를 확인한다.

### Task 4: k6 최종 검증과 보고서

**Files:**
- Create: `scripts/performance/change-diagnosis-idempotency.js`
- Create: `scripts/performance/run-change-diagnosis-load.sh`
- Modify: `docs/performance/resume-metrics-report.md`

**Interfaces:**
- Consumes: 실행 중인 MySQL·Redis·Spring·외부 API stub
- Produces: 두 시나리오의 원본 JSON과 검증된 이력서 문장

- [ ] 동일 변동 100건을 요청하고 DB·외부 stub 호출 횟수를 기록한다.
- [ ] 카카오 지연 테스트를 baseline과 같은 조건으로 재실행한다.
- [ ] 대상 테스트와 전체 backend 테스트를 실행한다.
- [ ] 환경, 원본 수치, 한계와 이력서 문장을 보고서에 기록한다.
