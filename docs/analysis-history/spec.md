# Spec

## Index
- 없음

## Goal
진단 이력에서 필요한 값만 조회하고 N+1을 제거하며, 모바일 목록에서 OFFSET 기반으로 최신 이력을 10건씩 이어 조회한다.

## Scope
- 포함: Querydsl DTO 프로젝션, 집·보고서 JOIN, 사용자·진단 유형·위험도 필터, OFFSET API, Android 추가 로딩, k6 비교 측정
- 제외: 전체 건수 조회, 페이지 번호 UI, 커서 페이징, 진단·보고서 생성 흐름 변경, 신규 의존성

## Design
- 진단 조건과 페이징에 따라 달라지는 쿼리를 조합하고 필드명·타입 오류를 컴파일 시점에 확인하도록 Querydsl을 사용한다. 생성자 프로젝션으로 `analysisId`, `createdAt`, `analysisType`, `pdfReportId`, `nickname`, `address`, `mainReason`, `riskLevel`, `ltvScore`만 조회한다.
- 진단 이력당 집 정보와 보고서가 단일 건이므로 계약 후 이력은 집을 INNER JOIN하고 보고서를 LEFT JOIN한다. 계약 전 이력은 분석의 이름·주소를 사용하고 보고서만 LEFT JOIN해 추가 쿼리를 없앤다.
- 등기 변동이 드물어 사용자별 이력이 적고 OFFSET의 조회 부담이 크지 않을 것으로 예상한다. 필터 변경 시 복합 커서 유효성 검증 없이 조회 위치만 0으로 초기화하도록 OFFSET 페이징을 사용한다.
- `GET /analyses`와 `GET /analyses/diff`는 `offset` 기본값 0과 선택적 `riskLevel`을 받는다. 페이지 크기는 10이며 응답은 `items`, `nextOffset`, `hasNext`를 반환한다.
- 통합 목록은 PRE·POST 각각에서 `offset + 11`건까지 조회해 기존 정렬 `(createdAt DESC, POST 우선, analysisId DESC)`로 병합한 후, 통합 결과에 OFFSET을 한 번만 적용한다. DIFF 목록은 저장된 `analysisType=DIFF` 조건으로 단일 쿼리에 OFFSET과 LIMIT 11을 적용하되, 응답 `analysisType`은 Android 보고서 매칭 계약에 맞춰 원본 유형인 `POST`를 유지한다.
- offset이 음수이거나 지원 범위를 넘으면 400을 반환한다. 필터는 OFFSET·LIMIT보다 먼저 적용한다.
- Android는 최초 진입·새로고침·탭·위험도 필터 변경 시 offset을 0으로 초기화하고, 목록 하단 도달 시 서버가 반환한 `nextOffset`으로 다음 10건을 붙인다. 중복 요청을 막고 실패 시 기존 목록과 offset을 유지한다.

## Test Plan
- 백엔드: 기본 offset, 다음 offset, 마지막·빈 페이지, 음수·범위 초과, PRE·POST 병합 순서, 필터 선적용, DIFF 조회 조건과 응답 `analysisType=POST` 계약을 검증한다.
- 조회 쿼리가 필요한 컬럼만 선택하고 집·보고서 접근으로 추가 SQL이 발생하지 않는지 검증한다.
- Android: 최초 조회, 다음 페이지 추가, 마지막 페이지, 중복 로딩 방지, 실패 재시도, 필터 변경 시 offset 초기화를 검증한다.
- k6로 진단 이력 1,000건, 동시 사용자 10명, 총 100회 요청을 동일 환경에서 비페이징 기준 커밋과 OFFSET 구현에 각각 실행해 SQL 실행 횟수와 최초 조회 시간을 비교한다.

## Notes
- OFFSET이 커서보다 DB 자원이나 메모리를 적게 사용한다는 의미는 아니다. 이력이 많거나 깊은 페이지 조회가 늘면 커서 방식 또는 DB 통합 조회를 재검토한다.
- 페이지 요청 사이에 데이터가 추가·삭제되면 중복·누락이 생길 수 있다. 새로고침과 항목 ID 기반 중복 제거로 화면을 다시 맞추되 스냅샷 일관성은 보장하지 않는다.
- 기준선 후보는 `08fcb4e`이며, 해당 커밋의 `/analyses`는 엔티티 전체 목록을 페이징 없이 반환한다. 측정 전 실제 소스와 실행 환경으로 다시 확인한다.
