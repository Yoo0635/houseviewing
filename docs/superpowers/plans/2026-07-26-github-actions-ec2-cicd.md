# GitHub Actions–EC2 CI/CD Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Validate the Spring Boot, FastAPI, and Android surfaces in CI, publish immutable backend images to GHCR, and provide an opt-in SSH deployment that becomes operational after EC2 GitHub Secrets are registered.

**Architecture:** Pull requests and pushes execute independent backend, Python, and Android jobs. A manual deployment workflow reruns server validation, publishes commit-SHA and `latest` images, and conditionally runs an SSH deployment script against an EC2 host. A separate production Compose file keeps MySQL, Redis, and FastAPI internal while exposing Spring Boot and verifying both application health endpoints.

**Tech Stack:** GitHub Actions, Java 17, Gradle 8.14/8.13, Python 3.11, pytest, Android SDK 36, Docker Buildx, GHCR, Docker Compose, OpenSSH, Spring Boot Actuator.

## Global Constraints

- Normal pull requests and pushes must never require EC2 credentials.
- EC2 deployment must run only from manual `workflow_dispatch` with `deploy=true`.
- Android is validated and uploaded as an APK artifact but is not deployed to EC2.
- No secret value may be committed; `.env.production.example` contains names and safe examples only.
- Production MySQL, Redis, and FastAPI ports must remain internal to the Docker network.
- Container images must carry both the immutable commit SHA and `latest` tags.
- Automatic rollback, TLS, DNS, EC2 provisioning, and Play Store publishing remain out of scope.

---

### Task 1: Backend Health Contract and Environment-Safe Configuration

**Files:**
- Modify: `backend/build.gradle`
- Modify: `backend/src/main/resources/application.yml`
- Modify: `backend/src/main/java/com/house/houseviewing/global/config/SecurityConfig.java`
- Create: `backend/src/test/java/com/house/houseviewing/global/health/HealthEndpointIntegrationTest.java`

**Interfaces:**
- Consumes: Existing Spring Boot application and stateless security filter chain.
- Produces: Unauthenticated `GET /actuator/health` returning HTTP 200 and environment-overridable datasource, Redis, AWS, Kakao, Python, and JWT properties.

- [ ] **Step 1: Add an integration test for the health endpoint**

```java
@SpringBootTest
@AutoConfigureMockMvc
class HealthEndpointIntegrationTest {
    @Autowired MockMvc mockMvc;

    @Test
    void healthEndpointIsPublic() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }
}
```

- [ ] **Step 2: Run the focused test and confirm the missing endpoint**

Run:

```bash
cd backend
SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3307/house_viewing \
SPRING_DATASOURCE_USERNAME=root \
SPRING_DATASOURCE_PASSWORD=1234 \
./gradlew test --tests '*HealthEndpointIntegrationTest' --no-daemon
```

Expected: FAIL because Actuator is not on the classpath or `/actuator/health` is not available.

- [ ] **Step 3: Add Actuator and expose only health/info**

Add `spring-boot-starter-actuator`, permit `"/actuator/health"` in `SecurityConfig`, and configure:

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info
  endpoint:
    health:
      probes:
        enabled: true
```

Replace literal operational settings with Spring environment placeholders while preserving local defaults, including `${JWT_SECRET:...}` and `${PYTHON_API_URL:http://localhost:8000}`.

- [ ] **Step 4: Start MySQL and Redis and rerun the focused test**

Run:

```bash
docker compose up -d db redis
cd backend
./gradlew test --tests '*HealthEndpointIntegrationTest' --no-daemon
```

Expected: PASS.

- [ ] **Step 5: Commit the backend health contract**

```bash
git add backend/build.gradle backend/src/main/resources/application.yml \
  backend/src/main/java/com/house/houseviewing/global/config/SecurityConfig.java \
  backend/src/test/java/com/house/houseviewing/global/health/HealthEndpointIntegrationTest.java
git commit -m "feat: add deployment health endpoint"
```

### Task 2: Production Compose and EC2 Deployment Script

**Files:**
- Modify: `.gitignore`
- Create: `.env.production.example`
- Create: `docker-compose.prod.yml`
- Create: `scripts/deploy-ec2.sh`

**Interfaces:**
- Consumes: GHCR image names through `GHCR_NAMESPACE`, repository name through `GHCR_REPOSITORY`, and immutable `IMAGE_TAG`.
- Produces: `scripts/deploy-ec2.sh <deploy-directory>` which validates required variables, pulls images, starts Compose, and waits for Spring/FastAPI health.

- [ ] **Step 1: Create a fixture environment and confirm production Compose is absent**

Run:

```bash
test ! -f docker-compose.prod.yml
```

Expected: exit 0.

- [ ] **Step 2: Add safe production configuration**

Add a tracked `.env.production.example` and an exception to `.gitignore`. Create `docker-compose.prod.yml` with:

- GHCR-backed Spring and FastAPI images.
- MySQL and Redis named volumes.
- Internal-only MySQL, Redis, and FastAPI networking.
- Health checks for MySQL, Redis, FastAPI, and Spring.
- Spring dependencies on all three healthy services.
- Environment-variable-only credentials.

- [ ] **Step 3: Add the deployment script**

The POSIX-compatible Bash script must:

```text
validate docker, docker compose, .env, GHCR_NAMESPACE, GHCR_REPOSITORY, IMAGE_TAG
docker compose pull
docker compose up -d --remove-orphans
poll http://127.0.0.1:8080/actuator/health
poll the python container's /health endpoint
print docker compose ps and exit non-zero on failure
```

- [ ] **Step 4: Validate shell and Compose syntax**

Run:

```bash
bash -n scripts/deploy-ec2.sh
cp .env.production.example .env.production.verify
docker compose --env-file .env.production.verify -f docker-compose.prod.yml config
rm .env.production.verify
```

Expected: both commands exit 0 and the rendered Compose model contains four services.

- [ ] **Step 5: Commit deployment runtime files**

```bash
git add .gitignore .env.production.example docker-compose.prod.yml scripts/deploy-ec2.sh
git commit -m "feat: add production compose deployment"
```

### Task 3: Continuous Integration Workflow

**Files:**
- Create: `.github/workflows/ci.yml`

**Interfaces:**
- Consumes: Backend and Android Gradle wrappers plus `python/requirements.txt`.
- Produces: Three independent GitHub checks and a downloadable debug APK.

- [ ] **Step 1: Add backend CI**

Use `actions/checkout@v6`, `actions/setup-java@v5`, and `gradle/actions/setup-gradle@v6`. Configure MySQL 8 and Redis 7 services, wait for health, and run:

```bash
cd backend
chmod +x gradlew
./gradlew clean test bootJar --no-daemon
```

Upload `backend/build/reports/tests/test` with `actions/upload-artifact@v7` on failure.

- [ ] **Step 2: Add Python CI**

Use `actions/setup-python@v6` with Python `3.11`, pip caching keyed by `python/requirements.txt`, install `requirements.txt` and pytest, then run:

```bash
cd python
pytest tests -q
```

- [ ] **Step 3: Add Android CI**

Use JDK 17, `android-actions/setup-android@v4`, and `gradle/actions/setup-gradle@v6`, install platform 36/build-tools 35, then run:

```bash
cd app
chmod +x gradlew
./gradlew testDebugUnitTest assembleDebug --no-daemon
```

Upload `app/app/build/outputs/apk/debug/app-debug.apk` with `actions/upload-artifact@v7`.

- [ ] **Step 4: Parse and inspect the workflow**

Run:

```bash
ruby -e 'require "yaml"; YAML.load_file(".github/workflows/ci.yml", aliases: true)'
```

Expected: exit 0.

- [ ] **Step 5: Commit CI**

```bash
git add .github/workflows/ci.yml
git commit -m "ci: validate backend python and android"
```

### Task 4: GHCR Publishing and Optional EC2 CD

**Files:**
- Replace: `.github/workflows/deploy.yml`

**Interfaces:**
- Consumes: `workflow_dispatch.inputs.deploy`, `GITHUB_TOKEN`, GHCR metadata, and EC2/production GitHub Secrets.
- Produces: Two GHCR images and, when enabled, a health-checked EC2 deployment.

- [ ] **Step 1: Replace the Hello World workflow**

Configure:

- Manual `workflow_dispatch`.
- Boolean `deploy` input defaulting to `false`.
- `contents: read` and `packages: write` permissions.
- Server validation jobs for backend and Python before publication.
- Docker login with `docker/login-action@v4`.
- Buildx with `docker/setup-buildx-action@v4`.
- Backend/Python image publishing with `docker/build-push-action@v7`.
- Lowercase GHCR image names generated from `github.repository_owner` and `github.event.repository.name`.

- [ ] **Step 2: Add deployment preflight**

When `deploy=true`, validate all required Secrets without printing their values:

```text
EC2_HOST EC2_USER EC2_SSH_KEY DEPLOY_PATH
MYSQL_ROOT_PASSWORD MYSQL_DATABASE MYSQL_USER MYSQL_PASSWORD
KAKAO_REST_API_KEY AWS_ACCESS_KEY AWS_SECRET_KEY JWT_SECRET
GHCR_USERNAME GHCR_READ_TOKEN
```

- [ ] **Step 3: Add SSH deployment**

The job must:

- Write the SSH key to a runner-temporary file with mode `600`.
- Add the target host to `known_hosts`.
- Create `DEPLOY_PATH` remotely.
- Copy `docker-compose.prod.yml` and `scripts/deploy-ec2.sh`.
- Stream the production `.env` to the remote host with mode `600`.
- Authenticate the remote Docker client to GHCR using `--password-stdin`.
- Execute `scripts/deploy-ec2.sh`.

- [ ] **Step 4: Parse and inspect the workflow**

Run:

```bash
ruby -e 'require "yaml"; YAML.load_file(".github/workflows/deploy.yml", aliases: true)'
```

Expected: exit 0.

- [ ] **Step 5: Commit CD**

```bash
git add .github/workflows/deploy.yml
git commit -m "ci: publish images and deploy to ec2"
```

### Task 5: Operations Documentation

**Files:**
- Create: `README.md`

**Interfaces:**
- Consumes: Both workflows, production Compose, deployment script, and required Secrets.
- Produces: Reproducible instructions for local validation, GHCR publication, and future EC2 activation.

- [ ] **Step 1: Document pipeline behavior**

Document:

- CI triggers and job responsibilities.
- Manual CD with `deploy=false` and `deploy=true`.
- Required GitHub Secrets.
- One-time EC2 prerequisites: Docker Engine, Compose plugin, inbound 22/8080 policy.
- GHCR classic PAT requirement for private package pull (`read:packages`).
- Local workflow-equivalent test commands.
- Manual rollback by dispatching the desired commit or setting `IMAGE_TAG` on the server.

- [ ] **Step 2: Validate documentation against files**

Run:

```bash
rg -n "deploy=false|deploy=true|EC2_HOST|GHCR_READ_TOKEN|docker-compose.prod.yml" README.md
```

Expected: all required operating concepts are documented.

- [ ] **Step 3: Commit documentation**

```bash
git add README.md
git commit -m "docs: document CI/CD operations"
```

### Task 6: Full Verification

**Files:**
- Verify all files from Tasks 1–5.

**Interfaces:**
- Consumes: Complete CI/CD implementation.
- Produces: Fresh evidence for tests, workflow syntax, shell syntax, Compose rendering, and image builds.

- [ ] **Step 1: Run backend tests and build**

```bash
docker compose up -d db redis
cd backend
./gradlew clean test bootJar --no-daemon
```

- [ ] **Step 2: Run Python tests**

```bash
cd python
python3 -m pytest tests -q
```

- [ ] **Step 3: Run Android tests and build**

```bash
cd app
./gradlew testDebugUnitTest assembleDebug --no-daemon
```

- [ ] **Step 4: Validate configuration files**

```bash
bash -n scripts/deploy-ec2.sh
ruby -e 'require "yaml"; YAML.load_file(".github/workflows/ci.yml", aliases: true); YAML.load_file(".github/workflows/deploy.yml", aliases: true)'
cp .env.production.example .env.production.verify
docker compose --env-file .env.production.verify -f docker-compose.prod.yml config
rm .env.production.verify
git diff --check
```

- [ ] **Step 5: Build deployment images**

```bash
docker build -t houseviewing-backend:verify backend
docker build -t houseviewing-python:verify python
```

- [ ] **Step 6: Review final change set**

```bash
git status --short
git log --oneline --max-count=8
```

Confirm every design requirement is represented and report any validation that could not run.
