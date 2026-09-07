# 무료 진단 동시성 k6 검증

## 조건

- 비교: 구현 전 `08fcb4e`, 현재 `94855ac` 및 `spring.jpa.open-in-view=false`
- 부하: 동시 요청 50개, Hikari 최대 커넥션 10개
- 외부 지연: 주소 조회·분석·PDF 생성 각각 800ms
- 격리 DB와 로컬 스텁을 사용해 각 외부 호출 횟수를 계수

## 결과

| 검증 | 구현 전 | 현재 | 개선 |
| --- | ---: | ---: | ---: |
| 서로 다른 UUID의 분석 호출 | 10회 | 1회 | 90% 감소 |
| 서로 다른 UUID의 PDF 생성 | 10회 | 1회 | 90% 감소 |
| 같은 UUID의 분석 호출 | 10회 | 1회 | 90% 감소 |
| 같은 UUID의 PDF 생성 | 10회 | 1회 | 90% 감소 |
| 요청당 평균 커넥션 사용 시간 | 1,053.10ms | 6.19ms | 99.41% 감소 |
| 최대 커넥션 사용 시간 | 2,477ms | 169ms | 93.18% 감소 |

- 서로 다른 UUID 50개: 현재 1건 완료, 49건 충돌 응답, 실패 0건
- 같은 UUID 50개: 현재 1건 완료, 49건 처리 중 응답, 실패 0건
- 완료된 UUID 50회 재요청: 모두 같은 완료 결과를 반환하고 외부 호출은 추가되지 않음
- 서로 다른 사용자 50명: 현재 50건 완료, 실패 0건, 관측된 대기 커넥션 0개

구현 전에는 커넥션 풀이 소진돼 Hikari 메트릭 요청도 일부 타임아웃됐다. 현재는 외부 호출을 트랜잭션 밖에서 수행하고 Open EntityManager in View를 비활성화해 외부 호출 대기 중 커넥션을 반환한다.

## 실행 파일

- `scripts/performance/free-diagnosis.js`
- `scripts/performance/free-diagnosis-stub.py`
- `scripts/performance/results/free-before-*.json`
- `scripts/performance/results/free-current-*.json`
