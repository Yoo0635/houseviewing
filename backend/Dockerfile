FROM eclipse-temurin:17-jdk-jammy AS builder

WORKDIR /app

COPY build.gradle settings.gradle ./
COPY gradle ./gradle
COPY gradlew gradlew.bat ./

RUN chmod +x gradlew && ./gradlew dependencies --no-daemon

COPY src ./src

RUN ./gradlew clean bootJar -x test --no-daemon

FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

RUN addgroup --system appgroup && adduser --system appuser --ingroup appgroup

COPY --from=builder /app/build/libs/*.jar app.jar

RUN chown appuser:appgroup app.jar

USER appuser

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
