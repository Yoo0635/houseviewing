# GitHub Actions–EC2 CI/CD Design

## Goal

Build a CI/CD pipeline that validates every application surface in the repository, publishes deployable Spring Boot and FastAPI container images to GitHub Container Registry (GHCR), and can deploy the production stack to a future EC2 instance by adding GitHub Secrets without changing the workflow.

## Current Context

- The repository contains three build surfaces:
  - `backend`: Spring Boot 3.5.10, Java 17, Gradle 8.14.
  - `python`: FastAPI on Python 3.11 with pytest tests and wkhtmltopdf runtime dependencies.
  - `app`: Android application using JDK 17 for its Gradle/AGP toolchain.
- The deployable server runtime consists of Spring Boot, FastAPI, MySQL, and Redis.
- Android produces an APK in CI but is not deployed to EC2.
- The existing `.github/workflows/deploy.yml` only prints `Hello World`.
- The existing root `docker-compose.yml` is a local-development composition and contains local credentials and host port exposure that must not be reused as production configuration.
- No EC2 instance currently exists. Deployment must therefore be opt-in and manually triggered.

## Chosen Architecture

Use two GitHub Actions workflows:

1. `ci.yml` validates pull requests and pushes.
2. `deploy.yml` publishes container images and optionally deploys them to EC2.

The deploy workflow accepts a boolean `deploy` input:

- `false`: build and publish Spring Boot and FastAPI images to GHCR.
- `true`: build and publish the images, copy production deployment files to EC2, and restart the stack over SSH.

This keeps CI usable without EC2 and makes the future deployment path operational after GitHub Secrets are registered.

## Continuous Integration

### Backend job

- Run on Ubuntu with Temurin JDK 17.
- Start MySQL 8 and Redis 7 service containers.
- Provide test-safe datasource, Redis, JWT, AWS, Kakao, and Python API environment variables.
- Run the Gradle test task with the repository wrapper.
- Upload JUnit reports when the job fails.

The S3 integration test uses Testcontainers/LocalStack and can use the Docker daemon available on the GitHub-hosted Ubuntu runner.

### Python job

- Run on Ubuntu with Python 3.11.
- Cache pip downloads using `python/requirements.txt`.
- Install application dependencies plus pytest.
- Run all tests under `python/tests`.

The current router tests mock PDF conversion and AI calls, so they do not require production API keys or wkhtmltopdf.

### Android job

- Run on Ubuntu with Temurin JDK 17 and the Android SDK.
- Cache Gradle dependencies.
- Run Android unit tests and build the debug APK.
- Upload the debug APK as a workflow artifact.

The Android application receives an empty Kakao key in CI because compilation does not require a live API call.

## Container Publishing

- Authenticate to `ghcr.io` with the workflow-provided `GITHUB_TOKEN`.
- Build the existing `backend/Dockerfile` and `python/Dockerfile` with Docker Buildx.
- Publish both immutable and convenience tags:
  - `ghcr.io/<owner>/<repository>-backend:<commit-sha>`
  - `ghcr.io/<owner>/<repository>-backend:latest`
  - `ghcr.io/<owner>/<repository>-python:<commit-sha>`
  - `ghcr.io/<owner>/<repository>-python:latest`
- Use GitHub Actions build cache for subsequent image builds.
- Do not publish an Android container image.

## EC2 Deployment

### Trigger

Deployment is only attempted when the manually supplied `deploy` input is `true`. Normal pushes and pull requests never require EC2 credentials.

### Transport

- Configure OpenSSH with `EC2_SSH_KEY`.
- Resolve the server using `EC2_HOST` and `EC2_USER`.
- Copy `docker-compose.prod.yml` and the deployment script into `DEPLOY_PATH`.
- Generate the production `.env` file on the server from GitHub Secrets with restrictive file permissions.

The EC2 instance name or AWS instance ID is not required for SSH deployment.

### Runtime

`docker-compose.prod.yml` runs:

- Spring Boot backend from GHCR.
- FastAPI service from GHCR.
- MySQL 8 with a persistent named volume.
- Redis 7 with a persistent named volume.

Only the Spring Boot port is exposed publicly by Compose. MySQL, Redis, and FastAPI communicate on the internal Docker network.

The Spring Boot service waits for healthy MySQL, Redis, and FastAPI services before starting.

### Health Verification

- FastAPI uses its existing `GET /health` endpoint.
- Spring Boot adds the Actuator health endpoint and exposes only `health` and `info`.
- After `docker compose up -d`, the deployment script waits for both services to become healthy.
- The deployment command exits non-zero when a service fails its health check so GitHub Actions reports a failed deployment.

Automatic rollback is outside this version. Immutable commit-SHA image tags preserve the artifact required for a manual rollback.

## Secrets and Configuration

The deploy workflow consumes:

- `EC2_HOST`
- `EC2_USER`
- `EC2_SSH_KEY`
- `DEPLOY_PATH`
- `MYSQL_ROOT_PASSWORD`
- `MYSQL_DATABASE`
- `MYSQL_USER`
- `MYSQL_PASSWORD`
- `KAKAO_REST_API_KEY`
- `AWS_ACCESS_KEY`
- `AWS_SECRET_KEY`
- `JWT_SECRET`
- `GHCR_USERNAME`
- `GHCR_READ_TOKEN`

The repository includes `.env.production.example` containing names and safe placeholders only. No secret value is committed.

The EC2 host uses `GHCR_READ_TOKEN` only to pull private images. Image publishing in GitHub Actions uses `GITHUB_TOKEN`.

## Failure Handling

- A failure in any CI job blocks that job and is visible before deployment.
- Image publication does not run unless backend and Python validation succeed.
- EC2 deployment does not run when `deploy=false`.
- Missing EC2 deployment secrets fail during a dedicated preflight validation step with the missing variable name.
- A failed image pull, Compose startup, or health check fails the CD job.
- Existing MySQL and Redis data survives container replacement through named volumes.

## Files

- Replace: `.github/workflows/deploy.yml`
- Create: `.github/workflows/ci.yml`
- Create: `docker-compose.prod.yml`
- Create: `.env.production.example`
- Create: `scripts/deploy-ec2.sh`
- Modify: `backend/build.gradle`
- Modify: `backend/src/main/resources/application.yml`
- Modify: `README.md`

## Verification

Local verification will include:

- Backend Gradle tests.
- Python pytest suite.
- Android unit-test/debug-APK build where the local SDK permits it.
- Backend and Python Docker image builds.
- Docker Compose production configuration rendering with a non-secret fixture environment.
- YAML parsing of both GitHub Actions workflows.
- Shell syntax validation of `scripts/deploy-ec2.sh`.

Actual EC2 SSH deployment cannot be executed until an instance and GitHub Secrets exist. The deploy workflow and scripts will instead be statically and locally validated.

## Explicit Non-Goals

- Creating or paying for an EC2 instance.
- Managing Route 53, TLS certificates, or a reverse proxy.
- Zero-downtime or blue-green deployment.
- Automatic rollback.
- Publishing the Android application to Google Play.
- Database backup automation or schema migration tooling.
