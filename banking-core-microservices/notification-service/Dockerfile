# notification-service — Multi-Stage Docker Build
# No HTTP server: WebApplicationType.NONE (spring-boot-starter-web absent).
# Build context: project root (set in docker-compose.yml via context: .)
# ─────────────────────────────────────────────────────────────────────────────

FROM maven:3.9-eclipse-temurin-8 AS build
WORKDIR /workspace

COPY pom.xml                        .
COPY config-server/pom.xml          config-server/pom.xml
COPY service-registry/pom.xml       service-registry/pom.xml
COPY api-gateway/pom.xml            api-gateway/pom.xml
COPY account-service/pom.xml        account-service/pom.xml
COPY transaction-service/pom.xml    transaction-service/pom.xml
COPY notification-service/pom.xml   notification-service/pom.xml

RUN mvn -B dependency:resolve -pl notification-service -am --no-transfer-progress -q 2>/dev/null || true

COPY notification-service/src notification-service/src

RUN mvn -B package -pl notification-service -am -DskipTests --no-transfer-progress -q

# ─────────────────────────────────────────────────────────────────────────────
# No EXPOSE: this service has no embedded HTTP server.
# Liveness is tracked via Eureka registration (visible at http://localhost:8761).
FROM eclipse-temurin:8-jre-alpine
VOLUME /tmp
COPY --from=build /workspace/notification-service/target/*.jar app.jar
ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "/app.jar"]
