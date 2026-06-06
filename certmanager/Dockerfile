# ═══════════════════════════════════════════════════════════
# Backend Dockerfile — Multi-stage build
# Java 21 + Spring Boot 3
# ═══════════════════════════════════════════════════════════

# Stage 1: Build
FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /app

# Copy pom.xml first — leverages Docker layer caching for dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the JAR (skip tests — run separately in CI)
RUN mvn package -DskipTests -B

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine AS runtime

# Security: run as non-root user
RUN addgroup -S certmanager && adduser -S certmanager -G certmanager

WORKDIR /app

# Copy the built JAR from the builder stage
COPY --from=builder /app/target/*.jar app.jar

# Change ownership
RUN chown -R certmanager:certmanager /app

USER certmanager

# Expose backend port
EXPOSE 8081

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget -q --spider http://localhost:8081/actuator/health || exit 1

# JVM optimizations for containers
ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", "app.jar"]
