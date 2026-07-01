# config-server — Multi-Stage Docker Build
# Build context: project root (set in docker-compose.yml via context: .)
# ─────────────────────────────────────────────────────────────────────────────

# ── Stage 1: Build ────────────────────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-8 AS build
WORKDIR /workspace

# Copy all module POMs first — Docker caches this layer until any POM changes.
# Maven needs the full reactor descriptor to process -pl correctly.
COPY pom.xml                        .
COPY config-server/pom.xml          config-server/pom.xml
COPY service-registry/pom.xml       service-registry/pom.xml
COPY api-gateway/pom.xml            api-gateway/pom.xml
COPY account-service/pom.xml        account-service/pom.xml
COPY transaction-service/pom.xml    transaction-service/pom.xml
COPY notification-service/pom.xml   notification-service/pom.xml

# Pre-download dependencies for this module only (cached until POM changes).
RUN mvn -B dependency:resolve -pl config-server -am --no-transfer-progress -q 2>/dev/null || true

# Copy only this module's source (layer invalidated only when this module changes).
COPY config-server/src config-server/src

# Package — skip tests (integration tests run in CI with embedded Kafka).
RUN mvn -B package -pl config-server -am -DskipTests --no-transfer-progress -q

# ── Stage 2: Runtime ──────────────────────────────────────────────────────────
FROM eclipse-temurin:8-jre-alpine

# /tmp volume for Spring Boot's embedded Tomcat temp files.
VOLUME /tmp

COPY --from=build /workspace/config-server/target/*.jar app.jar

EXPOSE 8888

# -Djava.security.egd speeds up SecureRandom initialisation inside containers.
ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "/app.jar"]
