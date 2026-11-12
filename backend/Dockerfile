# ─── Stage 1: Build executable JAR with Eclipse Temurin 21 + Maven Wrapper ───
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copy Maven wrapper and pom.xml first to leverage Docker layer caching
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN chmod +x mvnw && ./mvnw -B dependency:go-offline

# Copy source and build production JAR (tests run separately in CI/pre-deploy)
COPY src ./src
RUN ./mvnw -B clean package -DskipTests

# ─── Stage 2: Minimal non-root Java 21 JRE runtime for Render (512 MB RAM) ───
FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app

# Create non-root user and ensure upload directory is writable
RUN addgroup -S appgroup && adduser -S appuser -G appgroup \
    && mkdir -p /app/uploads \
    && chown -R appuser:appgroup /app

COPY --from=build --chown=appuser:appgroup /app/target/*.jar app.jar

USER appuser

# Default to production profile and conservative heap/metaspace limits for 512 MB RAM
ENV SPRING_PROFILES_ACTIVE=prod
ENV JAVA_OPTS="-Xms128m -Xmx288m -XX:MaxMetaspaceSize=112m -Xss512k -XX:+UseSerialGC -XX:+ExitOnOutOfMemoryError"

EXPOSE 8080

# Exec form via sh so PID 1 receives SIGTERM and Render's dynamic $PORT is honored
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -Dserver.port=${PORT:-8080} -jar /app/app.jar"]

