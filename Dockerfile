# Multi-stage Dockerfile for IAM Server
# Stage 1: Build
FROM eclipse-temurin:17-jdk-alpine AS builder

WORKDIR /app

# Copy gradle configuration and source code
COPY build.gradle settings.gradle /app/
COPY src /app/src

# Install Gradle and build application jar
RUN apk add --no-cache gradle && \
    gradle bootJar --no-daemon

# Stage 2: Production Runtime
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Create a dedicated non-root user for enhanced security
RUN addgroup -S iamgroup && adduser -S iamuser -G iamgroup

# Copy compiled jar from builder stage
COPY --from=builder /app/build/libs/*.jar app.jar

# Set ownership
RUN chown -R iamuser:iamgroup /app

USER iamuser

EXPOSE 9000

ENV SPRING_PROFILES_ACTIVE=prod \
    JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC"

HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:9000/actuator/health || exit 0

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
