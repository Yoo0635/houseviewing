# Kotlin / Android (AGP) — backend/Dockerfile과 같은 멀티스테이지 패턴(Gradle 빌드 → 산출물)
# 버전은 gradle/libs.versions.toml · app/build.gradle.kts 와 맞춤

ARG KOTLIN_VERSION=2.0.21
ARG AGP_VERSION=8.12.3
ARG COMPILE_SDK=36
ARG TARGETPLATFORM=linux/amd64

FROM --platform=${TARGETPLATFORM} eclipse-temurin:17-jdk-jammy AS builder

ARG KOTLIN_VERSION
ARG AGP_VERSION
ARG COMPILE_SDK
ARG TARGETPLATFORM

LABEL org.opencontainers.image.description="houseViewingApp Android (Kotlin ${KOTLIN_VERSION}, AGP ${AGP_VERSION}, compileSdk ${COMPILE_SDK})"

ENV DEBIAN_FRONTEND=noninteractive \
    ANDROID_SDK_ROOT=/opt/android-sdk \
    ANDROID_HOME=/opt/android-sdk \
    GRADLE_OPTS="-Dorg.gradle.daemon=false -Dkotlin.compiler.execution.strategy=in-process"

ENV PATH="${PATH}:${ANDROID_SDK_ROOT}/cmdline-tools/latest/bin:${ANDROID_SDK_ROOT}/platform-tools"

RUN apt-get update \
    && apt-get install -y --no-install-recommends wget unzip \
    && rm -rf /var/lib/apt/lists/*

ARG CMDLINE_TOOLS_URL=https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip
RUN wget -q "${CMDLINE_TOOLS_URL}" -O /tmp/cmdline-tools.zip \
    && unzip -q /tmp/cmdline-tools.zip -d /tmp \
    && mkdir -p "${ANDROID_SDK_ROOT}/cmdline-tools" \
    && mv /tmp/cmdline-tools "${ANDROID_SDK_ROOT}/cmdline-tools/latest" \
    && rm -f /tmp/cmdline-tools.zip

RUN yes | sdkmanager --licenses >/dev/null \
    && sdkmanager --install \
        "platform-tools" \
        "platforms;android-${COMPILE_SDK}" \
        "build-tools;35.0.0"

WORKDIR /workspace

COPY gradle ./gradle
COPY gradlew gradlew.bat ./
COPY settings.gradle.kts build.gradle.kts gradle.properties ./
COPY app ./app

RUN chmod +x gradlew \
    && ./gradlew :app:assembleDebug --no-daemon --stacktrace

FROM --platform=${TARGETPLATFORM} eclipse-temurin:17-jre-jammy

ARG TARGETPLATFORM

WORKDIR /out

COPY --from=builder /workspace/app/build/outputs/apk/debug/app-debug.apk ./app-debug.apk

CMD ["ls", "-la", "/out"]
