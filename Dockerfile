# Multi-Stage Build Dockerfile for IAM Server
# Stage 1: Build JAR using Gradle
FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /workspace

COPY build.gradle settings.gradle ./
COPY gradle gradle
COPY gradlew ./
COPY src src

# Make gradlew executable if present and build application JAR
RUN if [ -f "./gradlew" ]; then chmod +x ./gradlew && ./gradlew bootJar --no-daemon -x test; \
    else apk add --no-cache gradle && gradle bootJar --no-daemon -x test; fi

# Stage 2: Lightweight Minimal Runtime
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Run as non-root user for security best practice
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

COPY --from=builder /workspace/build/libs/*.jar app.jar
RUN chown -R appuser:appgroup /app

USER appuser

EXPOSE 9000

ENV SPRING_PROFILES_ACTIVE=prod \
    JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
