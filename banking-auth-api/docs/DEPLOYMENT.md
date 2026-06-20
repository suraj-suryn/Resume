# Deployment Guide — Banking Auth API

---

## Table of Contents

1. [Local Development (H2)](#1-local-development-h2)
2. [Docker — Single Container](#2-docker--single-container)
3. [Docker Compose — App + PostgreSQL](#3-docker-compose--app--postgresql)
4. [Environment Variables Reference](#4-environment-variables-reference)
5. [Production Checklist](#5-production-checklist)
6. [GitHub Actions CI](#6-github-actions-ci)

---

## 1. Local Development (H2)

Fastest way to run. Uses an embedded H2 file database — no external dependencies.

### Prerequisites

| Tool | Version |
|---|---|
| Java JDK | 8 or 11 |
| Maven | 3.6+ |

### Steps

```bash
# 1. Clone the repository
git clone https://github.com/suraj-suryn/Resume.git
cd Resume/banking-auth-api

# 2. Build and run
mvn spring-boot:run
```

The application starts on **port 8080**.

| URL | Description |
|---|---|
| `http://localhost:8080/swagger-ui.html` | Swagger UI (interactive API explorer) |
| `http://localhost:8080/actuator/health` | Health endpoint |
| `http://localhost:8080/h2-console` | H2 web console (dev only) |

**H2 Console login:**
- JDBC URL: `jdbc:h2:file:./data/bankingdb`
- Username: `sa`
- Password: *(leave blank)*

### Configuration (application.properties)

```properties
# Server
server.port=8080

# H2 datasource
spring.datasource.url=jdbc:h2:file:./data/bankingdb
spring.datasource.driver-class-name=org.h2.Driver

# JPA — auto-creates schema on startup
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false

# JWT — override these for security
jwt.secret=banking-auth-api-secret-key-256-bits-min-for-hmacsha256
jwt.expiration.ms=3600000
jwt.refresh.expiration.ms=86400000
```

---

## 2. Docker — Single Container

Build and run just the API container. Still uses H2 internally.

### Prerequisites

- Docker Desktop

### Steps

```bash
cd banking-auth-api

# Build the image
docker build -t banking-auth-api:latest .

# Run the container
docker run -p 8080:8080 \
  -e JWT_SECRET=your-very-secure-secret-key-at-least-32-chars \
  banking-auth-api:latest
```

### Dockerfile overview

```dockerfile
# Stage 1 — build
FROM eclipse-temurin:11-jdk-alpine AS build
RUN apk add --no-cache maven
COPY . .
RUN mvn clean package -DskipTests

# Stage 2 — runtime (smaller image, no JDK)
FROM eclipse-temurin:11-jre-alpine
COPY --from=build target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

The multi-stage build keeps the final image lean by discarding Maven, JDK, and build artifacts.

---

## 3. Docker Compose — App + PostgreSQL

Full production-like stack. Uses PostgreSQL as the database.

### Prerequisites

- Docker Desktop (with Compose v2)

### Steps

```bash
cd banking-auth-api

# Create a .env file with your secrets (never commit this file)
cat > .env << EOF
DB_USERNAME=bankuser
DB_PASSWORD=bankpass
JWT_SECRET=your-very-secure-secret-key-at-least-32-chars-long
EOF

# Start the stack
docker-compose up --build
```

To run in the background:

```bash
docker-compose up --build -d
```

To stop:

```bash
docker-compose down
```

To stop and remove volumes (wipes database):

```bash
docker-compose down -v
```

### docker-compose.yml overview

```yaml
services:
  postgres:
    image: postgres:14-alpine
    environment:
      POSTGRES_DB: bankingdb
      POSTGRES_USER: ${DB_USERNAME}
      POSTGRES_PASSWORD: ${DB_PASSWORD}
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U ${DB_USERNAME}"]
      interval: 10s
      retries: 5

  banking-auth-api:
    build: .
    ports:
      - "8080:8080"
    environment:
      SPRING_PROFILES_ACTIVE: prod
      DATABASE_URL: jdbc:postgresql://postgres:5432/bankingdb
      DB_USERNAME: ${DB_USERNAME}
      DB_PASSWORD: ${DB_PASSWORD}
      JWT_SECRET: ${JWT_SECRET}
    depends_on:
      postgres:
        condition: service_healthy
```

The `depends_on: condition: service_healthy` ensures the app does **not** start until PostgreSQL is fully ready to accept connections.

---

## 4. Environment Variables Reference

### Required in Production

| Variable | Description | Example |
|---|---|---|
| `DATABASE_URL` | PostgreSQL JDBC connection URL | `jdbc:postgresql://localhost:5432/bankingdb` |
| `DB_USERNAME` | Database username | `bankuser` |
| `DB_PASSWORD` | Database password | `strong_password_here` |
| `JWT_SECRET` | HS256 signing secret — **minimum 32 characters** | `my-super-secret-key-for-jwt-signing-256bit` |

### Optional Overrides

| Variable | Default | Description |
|---|---|---|
| `JWT_EXPIRY_MS` | `3600000` | Access token lifetime in milliseconds (1 hour) |
| `REFRESH_EXPIRY_MS` | `86400000` | Refresh token lifetime in milliseconds (24 hours) |
| `SERVER_PORT` | `8080` | Application port |

### application-prod.properties

```properties
spring.datasource.url=${DATABASE_URL}
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}
spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect
spring.jpa.hibernate.ddl-auto=update

jwt.secret=${JWT_SECRET}
jwt.expiration.ms=${JWT_EXPIRY_MS:3600000}
jwt.refresh.expiration.ms=${REFRESH_EXPIRY_MS:86400000}
```

> **Security note:** Never hardcode secrets in `application-prod.properties`. Always inject via environment variables or a secrets manager (AWS Secrets Manager, Vault, etc.).

---

## 5. Production Checklist

Before deploying to a real environment, review the following:

### Security

- [ ] **JWT secret** is at least 32 characters, randomly generated, and stored in a secrets manager
- [ ] **Database password** is strong and not committed to source control
- [ ] `spring.jpa.show-sql` is `false` (no SQL in production logs)
- [ ] H2 console (`/h2-console`) is disabled — it is only available in the `dev` profile
- [ ] HTTPS / TLS is terminated at the load balancer or reverse proxy (Nginx/AWS ALB)
- [ ] `spring.jpa.hibernate.ddl-auto` is set to `validate` (not `update`) once schema is stable

### Database

- [ ] PostgreSQL is accessible from the app container/host
- [ ] Database backups are configured
- [ ] Connection pool settings are tuned (HikariCP defaults are fine for medium load)

### Observability

- [ ] `/actuator/health` is accessible for health checks
- [ ] Log aggregation is configured (ELK, CloudWatch, etc.)
- [ ] Audit logs from `AuditLoggingFilter` are being captured

### Performance

- [ ] JVM heap is sized appropriately: `-Xmx512m` is a reasonable starting point
- [ ] Database indexes exist on `username`, `email`, `accountNumber`

---

## 6. GitHub Actions CI

The pipeline at `.github/workflows/ci.yml` runs automatically on every push to `main` or `develop`.

### Pipeline stages

```
push to main/develop
       │
       ▼
┌─────────────────┐
│  Build          │  mvn clean package -DskipTests
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│  Test           │  mvn test (13 tests)
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│  Docker Build   │  docker build -t banking-auth-api .
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ Upload Reports  │  test-results artifact (surefire XML)
└─────────────────┘
```

### Viewing test results

1. Go to **Actions** tab on GitHub
2. Click any workflow run
3. Download the `test-results` artifact to view Surefire XML reports

### Running CI locally

```bash
# Same commands the CI pipeline runs
mvn clean package -DskipTests
mvn test
docker build -t banking-auth-api .
```
