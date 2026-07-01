# account-service — Multi-Stage Docker Build
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

RUN mvn -B dependency:resolve -pl account-service -am --no-transfer-progress -q 2>/dev/null || true

COPY account-service/src account-service/src

RUN mvn -B package -pl account-service -am -DskipTests --no-transfer-progress -q

# ─────────────────────────────────────────────────────────────────────────────
FROM eclipse-temurin:8-jre-alpine
VOLUME /tmp
COPY --from=build /workspace/account-service/target/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "/app.jar"]
