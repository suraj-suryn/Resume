# Multi-stage build — Maven compiles inside Docker, so no local Maven install needed
FROM eclipse-temurin:11-jdk-alpine AS build
WORKDIR /app

COPY pom.xml .
COPY src ./src

# Download dependencies first (cached layer if pom.xml unchanged)
RUN apk add --no-cache maven && mvn dependency:go-offline -q

# Build the fat JAR
RUN mvn clean package -DskipTests -q

# ── Runtime image (JRE only — smaller) ───────────────────────
FROM eclipse-temurin:11-jre-alpine
WORKDIR /app

COPY --from=build /app/target/banking-auth-api-1.0.0.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
