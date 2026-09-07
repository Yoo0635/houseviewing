# Spec

## Index
- 없음

## Goal
무료 사용자의 사전 등기부 진단을 한 번만 허용하고, 버튼 연타나 네트워크 재시도에도 분석과 PDF를 중복 생성하지 않는다.

## Scope
- 포함: 무료 진단 상태·단계 관리, 사용자 단위 원자적 선점, 요청 UUID 기반 멱등성
- 포함: 카카오 주소 조회·분석·PDF 생성의 트랜잭션 분리, 실패 단계 재시도, 중단된 작업 복구
- 포함: `PreAnalysisController`, `AnalysisQueryService`, `PreAnalysisService`, `PreReportService`, `SubscriptionEntity`와 관련 저장소·테스트 변경
- 포함: Android 요청 UUID 전달과 빠른 진단 버튼 재탭 방지
- 제외: 프리미엄 진단 횟수 정책 변경, 메시지 큐 기반 비동기 처리

## Design

### 상태 모델
- `FreeDiagnosisStatus`는 `AVAILABLE`, `PROCESSING`, `COMPLETED`, `FAILED`를 가진다.
- `FreeDiagnosisStage`는 `ADDRESS`, `ANALYSIS`, `PDF`를 가진다.
- `SubscriptionEntity`에 무료 진단 상태, 현재 단계, 요청 UUID, 작업 만료 시각을 저장한다. 무료 구독 생성 시 상태는 `AVAILABLE`이다.
- `PreAnalysisEntity`에 요청 UUID를 저장해 분석 성공 후 PDF만 실패한 요청은 기존 분석을 재사용한다.

### 진단 선점과 횟수 차감
- 무료 진단 시작은 조회 후 변경하지 않고 `userId`와 `status=AVAILABLE`을 조건으로 한 업데이트 한 번으로 `PROCESSING` 상태를 선점한다.
- 조건부 업데이트 결과가 1이면 진단을 시작하고, 0이면 현재 상태와 요청 UUID를 확인해 중복 요청 또는 무료 진단 사용 완료로 응답한다.
- 별도의 애플리케이션 락이나 Redis 락은 사용하지 않는다. DB의 원자적 조건부 업데이트와 사용자별 구독 행의 유일성을 최종 제약으로 사용한다.
- 분석과 PDF 생성이 모두 완료된 경우에만 `COMPLETED`로 변경해 무료 횟수를 사용한 것으로 처리한다.

### 멱등성
- `PreAnalysisController`는 `Idempotency-Key` 헤더로 UUID를 필수 입력받고 UUID 형식을 검증한다.
- Android는 한 번의 진단 작업에 UUID를 한 번 생성하고 버튼 연타와 네트워크 재시도에서 같은 값을 재사용한다.
- 같은 UUID가 `PROCESSING`이면 새 작업을 실행하지 않고 현재 단계를 반환하고, `COMPLETED`이면 기존 `PdfDownloadResponse`를 반환한다.
- 다른 UUID가 `PROCESSING`이면 진행 중인 무료 진단이 있음을 반환하고, `COMPLETED`이면 기존 `FREE_DIAGNOSIS_ALREADY_USED` 흐름을 따른다.
- `FAILED` 요청은 저장된 단계부터 재시도한다. `PDF` 단계 실패는 저장된 분석을 재사용해 PDF만 다시 생성한다.

### 트랜잭션 경계
- `AnalysisQueryService.executePreContractDiagnosis`는 전체 흐름을 조정하되 트랜잭션을 열지 않는다.
- `AVAILABLE`에서 `PROCESSING`으로의 선점, 단계 변경, 분석·PDF 메타데이터 저장, `COMPLETED` 또는 `FAILED` 변경만 각각 짧은 트랜잭션으로 처리한다.
- 카카오 주소 조회, Python 분석 엔진 호출, PDF 생성·업로드는 트랜잭션 밖에서 실행한다.
- 트랜잭션 메서드는 별도 Spring 빈을 통해 호출해 프록시 기반 트랜잭션 경계가 적용되도록 한다.

### 실패와 복구
- 외부 호출 실패 시 현재 단계를 유지한 채 `FAILED`로 변경하고 무료 횟수는 차감하지 않는다.
- 분석 저장 후 PDF 생성이 실패하면 분석 결과와 요청 UUID를 보존하고 같은 UUID의 재시도에서 PDF 단계만 실행한다.
- 서버 종료로 `PROCESSING`이 남는 경우 작업 만료 시각이 지난 요청만 재선점할 수 있다. 재선점과 완료 변경은 현재 요청 UUID와 상태를 조건으로 처리해 이전 작업의 늦은 저장을 거절한다.
- 상태 변경에 실패하거나 조건부 업데이트 결과가 0이면 외부 호출을 시작하지 않는다.

## Test Plan
- 동시에 같은 무료 사용자의 서로 다른 UUID 요청을 보내면 조건부 업데이트 하나만 성공하고 분석·PDF 외부 호출과 결과 저장이 각각 한 번인지 검증
- 같은 UUID를 반복하면 `PROCESSING`에서는 현재 단계, `COMPLETED`에서는 기존 PDF 식별자를 반환하며 외부 호출이 추가되지 않는지 검증
- 분석 실패 시 `FAILED`가 되고 무료 횟수가 차감되지 않으며, 같은 UUID 재시도에서 분석 단계부터 다시 실행되는지 검증
- PDF 실패 시 기존 분석이 한 건만 유지되고, 같은 UUID 재시도에서 PDF만 다시 생성되는지 검증
- 만료되지 않은 `PROCESSING` 요청은 재선점할 수 없고 만료된 요청만 재선점할 수 있는지 검증
- 카카오 주소 조회·분석·PDF 생성 시 활성 트랜잭션이 없고, 상태 및 결과 저장 시에만 트랜잭션이 활성화되는지 검증
- 프리미엄 사용자는 기존처럼 반복 진단할 수 있는지 검증

## Notes
- 조건부 업데이트는 명시적 분산 락을 대체하지만 DB는 업데이트 동안 해당 행의 잠금을 내부적으로 사용한다.
- UUID는 파일 내용의 동일성이 아니라 하나의 논리적 진단 요청을 식별한다. 파일 해시는 멱등성 키로 사용하지 않는다.
- 진행 상태 응답은 `requestId`, `status`, `stage`를 포함하고 완료 응답은 기존 PDF 식별자와 경로를 포함한다.
