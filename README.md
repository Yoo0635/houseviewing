# HouseViewing

등기부등본 변동을 감지하고 위험도를 분석해 대응 방안을 제공하는 서비스입니다.

## 구성

- `backend`: Spring Boot API, MySQL, Redis, S3 연동
- `python`: FastAPI 기반 OCR·위험 분석·PDF 생성 엔진
- `app`: Android 클라이언트

운영 환경에서는 Spring Boot, FastAPI, MySQL, Redis를 Docker Compose로 실행합니다. 외부에는 Spring Boot의 `8080` 포트만 노출하고, 나머지 서비스는 Docker 내부 네트워크로 통신합니다.

## CI

[`.github/workflows/ci.yml`](.github/workflows/ci.yml)은 `main`·`develop` 대상 Pull Request와 해당 브랜치 Push에서 다음 작업을 병렬 수행합니다.

- Backend: JDK 17, MySQL 8, Redis 7 환경에서 Gradle 테스트와 Boot JAR 생성
- Python: Python 3.11 의존성 설치 후 전체 pytest 실행
- Android: 단위 테스트 후 Debug APK 생성 및 Actions Artifact 업로드

Backend 테스트 실패 시 테스트 리포트를 Artifact로 보존합니다.

## CD

[`.github/workflows/deploy.yml`](.github/workflows/deploy.yml)은 GitHub Actions의 `Run workflow`로 수동 실행합니다.

- `deploy=false`(기본값): Backend와 Python을 검증하고, 커밋 SHA 및 `latest` 태그로 GHCR에 이미지를 발행합니다.
- `deploy=true`: 이미지 발행 후 EC2에 운영 Compose 파일을 전송하고, 새 이미지를 기동한 뒤 Spring Boot와 FastAPI의 헬스 체크를 검증합니다.

EC2 인스턴스 이름이나 AWS 인스턴스 ID는 필요하지 않습니다. SSH 접속에 사용할 호스트와 사용자만 GitHub Secrets로 설정합니다.

### 발행 이미지

```text
ghcr.io/<owner>/<repository>-backend:<commit-sha>
ghcr.io/<owner>/<repository>-backend:latest
ghcr.io/<owner>/<repository>-python:<commit-sha>
ghcr.io/<owner>/<repository>-python:latest
```

### EC2 사전 조건

- Docker Engine과 Docker Compose v2가 설치되어 있어야 합니다.
- 배포 사용자가 `docker` 명령을 실행할 수 있어야 합니다.
- 보안 그룹과 네트워크에서 GitHub Actions Runner의 SSH 접속이 가능해야 합니다.
- 애플리케이션 접근이 필요하면 `8080` 포트를 허용해야 합니다.

### 필수 GitHub Secrets

| 이름 | 용도 / 예시 |
| --- | --- |
| `EC2_HOST` | EC2 Public IP 또는 DNS |
| `EC2_USER` | `ubuntu`, `ec2-user` 등 SSH 사용자 |
| `EC2_SSH_KEY` | EC2 접속용 개인 키 전문 |
| `DEPLOY_PATH` | `/opt/houseviewing`과 같은 절대 경로 |
| `GHCR_USERNAME` | GHCR 이미지 Pull 권한이 있는 사용자 |
| `GHCR_READ_TOKEN` | `read:packages` 권한의 토큰 |
| `MYSQL_ROOT_PASSWORD` | MySQL root 비밀번호 |
| `MYSQL_DATABASE` | 운영 데이터베이스 이름 |
| `MYSQL_USER` | 애플리케이션 DB 사용자 |
| `MYSQL_PASSWORD` | 애플리케이션 DB 비밀번호 |
| `KAKAO_REST_API_KEY` | Kakao REST API 키 |
| `AWS_ACCESS_KEY` | S3 접근 키 |
| `AWS_SECRET_KEY` | S3 비밀 키 |
| `JWT_SECRET` | 32자 이상의 JWT 서명 키 |
| `API_URL` | OCR API Endpoint |
| `SECRET_KEY` | OCR API Secret |
| `RTMS_SERVICE_KEY` | 공공데이터포털 실거래가 Service Key |
| `RTMS_RH_TRADE_URL` | 연립·다세대 실거래가 API URL |
| `RTMS_SH_TRADE_URL` | 아파트 실거래가 API URL |
| `GROQ_API_KEY` | 분석 문구 생성용 Groq API 키 |

### 선택 GitHub Variables

설정하지 않으면 아래 기본값을 사용합니다.

| 이름 | 기본값 |
| --- | --- |
| `AWS_REGION` | `ap-northeast-2` |
| `AWS_S3_BUCKET` | `house-viewing-storage` |
| `GROQ_MODEL` | `llama-3.3-70b-versatile` |
| `GEMINI_FALLBACK_ENABLED` | `false` |
| `PROPERTY_TYPE` | 빈 값 |

## 운영 Compose 검증

실제 비밀값은 커밋하지 않고 [`.env.production.example`](.env.production.example)을 복사해 사용합니다.

```bash
cp .env.production.example .env.production.local
docker compose \
  --env-file .env.production.local \
  -f docker-compose.prod.yml \
  config
```

운영 배포 스크립트는 EC2의 `.env`와 SHA 이미지 태그를 읽어 이미지를 교체하고 두 애플리케이션의 헬스 체크가 성공할 때까지 대기합니다.

```bash
bash scripts/deploy-ec2.sh /opt/houseviewing
```

자동 롤백은 수행하지 않습니다. 문제가 발생하면 EC2의 `.env`에서 `IMAGE_TAG`를 이전 커밋 SHA로 변경하고 같은 배포 스크립트를 다시 실행할 수 있습니다.
