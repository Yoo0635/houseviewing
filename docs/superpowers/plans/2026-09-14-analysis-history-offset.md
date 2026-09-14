# Analysis History OFFSET Pagination Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the unpaged diagnosis-history endpoint with Querydsl projection/JOIN queries and 10-item OFFSET pagination, wire Android infinite scrolling, and measure it against the unpaged baseline.

**Architecture:** Backend query repositories return scalar `AnalysisHistoryRow` values. The service merges PRE and POST rows once, applies the unified offset, and returns `nextOffset`; Android owns one paging state per tab/filter and requests the next page at the list bottom. The performance harness seeds 50 histories and executes 100 requests with 10 virtual users against baseline and optimized commits.

**Tech Stack:** Java 21, Spring Boot, JPA, Querydsl 5.1 Jakarta, Kotlin Android, Retrofit, JUnit/AssertJ, k6, MySQL

**Spec:** `docs/analysis-history/spec.md`

## Global Constraints

- Page size is exactly 10; fetch 11 candidates to determine `hasNext` without a COUNT query.
- `/analyses` preserves `(createdAt DESC, POST before PRE, analysisId DESC)` and applies OFFSET once after merging PRE and POST.
- `/analyses/diff` filters stored analyses by `analysisType=DIFF`, but response `analysisType` stays as source type `POST`; user and optional risk filters apply before paging.
- Projection excludes `rawData` and report bodies; JOINs must not trigger per-row SQL.
- Android resets offset to 0 when tab or risk filter changes and uses the server-provided `nextOffset` for scrolling.
- k6 validation uses 50 histories, 10 concurrent users, and exactly 100 requests for both baseline and optimized runs.
- No new dependency beyond Querydsl already specified; commits use concise Korean messages.

---

### Task 1: Backend projection, JOIN, and OFFSET contract

**Files:**
- Modify: `backend/build.gradle`
- Create: `backend/src/main/java/com/house/houseviewing/api/query/dto/AnalysisHistoryRow.java`
- Create: `backend/src/main/java/com/house/houseviewing/api/query/dto/AnalysisHistoryPageResponse.java`
- Create: `backend/src/main/java/com/house/houseviewing/api/query/repository/AnalysisHistoryQueryRepository.java`
- Modify: `backend/src/main/java/com/house/houseviewing/api/query/controller/AnalysisQueryController.java`
- Modify: `backend/src/main/java/com/house/houseviewing/api/query/service/AnalysisQueryService.java`
- Modify: `backend/src/main/java/com/house/houseviewing/domain/analysis/postanalysis/dto/response/AnalysisResponse.java`
- Modify: `backend/src/main/java/com/house/houseviewing/domain/analysis/postanalysis/service/PostAnalysisService.java`
- Modify: `backend/src/main/java/com/house/houseviewing/domain/analysis/preanalysis/service/PreAnalysisService.java`
- Test: `backend/src/test/java/com/house/houseviewing/api/query/AnalysisQueryServiceTest.java`

**Interfaces:**
- Produces: `getAnalyses(Long userId, long offset, RiskLevel riskLevel)` and `getDiffAnalyses(Long userId, long offset, RiskLevel riskLevel)` returning `AnalysisHistoryPageResponse`.
- Produces: `AnalysisHistoryPageResponse(List<AnalysisResponse> items, Long nextOffset, boolean hasNext)`.
- Produces: repository methods accepting filters plus `long offset`/`long limit`; the unified endpoint requests `offset + 11` rows from each source and applies the offset once in the service.

- [ ] **Step 1: Write failing service and controller tests**

Add tests that expect a default offset of 0, a first page of 10 with `nextOffset=10`, a final page with null `nextOffset`, unified PRE/POST sorting before offset, risk filtering before paging, DIFF-only lookup, and rejection of negative or larger-than-supported offsets.

```java
assertThat(result.items()).hasSize(10);
assertThat(result.nextOffset()).isEqualTo(10L);
assertThat(result.hasNext()).isTrue();
```

- [ ] **Step 2: Run tests and verify RED**

Run: `GRADLE_USER_HOME=/tmp/gradle-history-backend sh gradlew test --tests 'com.house.houseviewing.api.query.AnalysisQueryServiceTest' --no-daemon`

Expected: compilation/test failure because the OFFSET signatures and response do not exist.

- [ ] **Step 3: Implement the minimum backend change**

Use Querydsl constructor projection and JOINs. Validate offset at the service boundary, fetch 11 candidates, and compute the next position from the request offset plus the number returned.

```java
long nextOffset = Math.addExact(offset, pageRows.size());
return new AnalysisHistoryPageResponse(items, hasNext ? nextOffset : null, hasNext);
```

- [ ] **Step 4: Run targeted and full backend tests**

Run the targeted command from Step 2, then `GRADLE_USER_HOME=/tmp/gradle-history-backend sh gradlew test --no-daemon`.

- [ ] **Step 5: Commit**

```bash
git add backend/build.gradle backend/src/main backend/src/test/java/com/house/houseviewing/api/query/AnalysisQueryServiceTest.java
git commit -m "진단 이력 OFFSET 조회 구현"
```

### Task 2: Android infinite-scroll OFFSET client

**Files:**
- Modify: `app/app/src/main/java/com/capstone/houseviewingapp/analysis/model/AnalysisDtos.kt`
- Modify: `app/app/src/main/java/com/capstone/houseviewingapp/data/remote/api/AnalysisApi.kt`
- Modify: `app/app/src/main/java/com/capstone/houseviewingapp/analysis/AnalysisRepository.kt`
- Modify: `app/app/src/main/java/com/capstone/houseviewingapp/analysis/RemoteAnalysisRepository.kt`
- Modify: `app/app/src/main/java/com/capstone/houseviewingapp/analysis/MockAnalysisRepository.kt`
- Modify: `app/app/src/main/java/com/capstone/houseviewingapp/analysis/AnalysisFragment.kt`
- Modify: `app/app/src/main/java/com/capstone/houseviewingapp/analysis/AnalysisRecordItem.kt`
- Modify: `app/app/src/main/java/com/capstone/houseviewingapp/analysis/AnalysisAdapter.kt`
- Modify: `app/app/src/main/res/layout/fragment_analysis.xml`
- Test: `app/app/src/test/java/com/capstone/houseviewingapp/analysis/AnalysisPagingRepositoryTest.kt`

**Interfaces:**
- Consumes: backend `items`, `nextOffset`, `hasNext` response and optional `riskLevel` request.
- Produces: repository methods `getAnalyses(accessToken, offset, riskLevel)` and `getDiffAnalyses(accessToken, offset, riskLevel)` returning `Result<AnalysisHistoryPageResponse>`.

- [ ] **Step 1: Write failing paging tests**

Cover parsing `nextOffset`, forwarding offset/risk filters, replacing the list at offset 0, appending later pages, suppressing concurrent loads, preserving state after failure, stopping at `hasNext=false`, and resetting to offset 0 after a tab/filter change.

```kotlin
assertEquals(10L, first.nextOffset)
assertFalse(last.hasNext)
```

- [ ] **Step 2: Run tests and verify RED**

Run: `GRADLE_USER_HOME=/tmp/gradle-history-android sh gradlew testDebugUnitTest --no-daemon`

Expected: compilation/test failure because the response wrapper and OFFSET parameters do not exist.

- [ ] **Step 3: Implement minimum Android paging**

Forward `offset` and `riskLevel` with Retrofit, keep paging state per tab/filter, use the server `nextOffset`, add a RecyclerView bottom listener, and preserve the existing list when a load fails.

- [ ] **Step 4: Run Android tests and build**

Run: `GRADLE_USER_HOME=/tmp/gradle-history-android sh gradlew testDebugUnitTest assembleDebug lintDebug --no-daemon`.

- [ ] **Step 5: Commit**

```bash
git add app/app/src/main app/app/src/test
git commit -m "Android 진단 이력 추가 조회 연동"
```

### Task 3: Reproducible k6 baseline and comparison report

**Files:**
- Create: `backend/src/main/resources/application-performance.yml`
- Modify: `backend/src/main/java/com/house/houseviewing/global/config/SecurityConfig.java`
- Create: `scripts/performance/analysis-history-n1.js`
- Create: `scripts/performance/run-analysis-history.sh`
- Create: `scripts/performance/seed-analysis-history.sql`
- Create: `docs/performance/analysis-history-report.md`

**Interfaces:**
- Consumes: login endpoint and `/analyses?offset=0`; baseline commit `08fcb4e` returns an unpaged array.
- Produces: JSON summaries for baseline and optimized runs plus a report containing raw SQL counts, first-request duration, and calculated reduction percentages.

- [ ] **Step 1: Add a failing harness self-check**

Make the runner reject any configuration other than 50 seeded histories, `VUS=10`, and `ITERATIONS=100`; make the k6 script check both array baseline and page-object optimized responses.

```sh
test "$VUS" -eq 10
test "$ITERATIONS" -eq 100
test "$HISTORY_COUNT" -eq 50
```

- [ ] **Step 2: Verify the self-check fails before configuration is present**

Run the runner without required environment/server setup and confirm it exits before k6.

- [ ] **Step 3: Implement seeding, run orchestration, and measurement output**

Seed exactly 25 PRE and 25 POST histories for the performance user. Capture Hibernate/DB SQL counts using the same mechanism for both commits, warm up both servers equally, then execute exactly 100 requests with 10 VUs. Calculate percentages as `(before - after) / before * 100`, rounded to two decimals.

- [ ] **Step 4: Run baseline and optimized measurements**

Run the harness against `08fcb4e` and the branch HEAD with identical database, JVM, warmup, and k6 settings. Do not invent values when the environment cannot run; retain raw JSON/log evidence.

- [ ] **Step 5: Write report and commit**

Use this exact result sentence shape with measured values:

```text
이력 조회 SQL 실행 {before}회 → {after}회({reduction}% 감소), 페이징 적용한 최초 조회 {beforeMs}ms → {afterMs}ms({reduction}% 감소)
```

Commit:

```bash
git add backend/src/main/resources/application-performance.yml backend/src/main/java/com/house/houseviewing/global/config/SecurityConfig.java scripts/performance docs/performance/analysis-history-report.md
git commit -m "진단 이력 조회 성능 비교 문서화"
```

### Task 4: Final verification and PR

**Files:**
- Verify all changed files

**Interfaces:**
- Consumes: Tasks 1-3 commits and raw performance evidence.
- Produces: reviewed branch, pushed remote branch, and pull request against `main`.

- [ ] **Step 1: Run final verification**

Run backend full tests/build, Android unit tests/build/lint, `git diff --check`, and the fixed k6 scenario. Confirm the performance report matches raw evidence.

- [ ] **Step 2: Update graph and inspect diff**

Run `graphify update .` only if `graphify-out/graph.json` exists. Review commit boundaries and ensure no unrelated files are included.

- [ ] **Step 3: Request whole-branch code review and address findings**

Review the full `main...HEAD` diff against this plan and `docs/analysis-history/spec.md`.

- [ ] **Step 4: Push and open PR**

Push `codex/analysis-history-offset` and create a PR against `main` with behavior, validation, benchmark conditions, and measured results.
