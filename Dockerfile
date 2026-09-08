# Production Minimal Runtime Dockerfile for IAM Server
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Run as non-root user for security best practice
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

COPY build/libs/*.jar app.jar
RUN chown -R appuser:appgroup /app

USER appuser

EXPOSE 9000

ENV SPRING_PROFILES_ACTIVE=prod \
    JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
