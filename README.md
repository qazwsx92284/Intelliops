# IntelliOps – AI-Powered Incident Analyzer

A personal proof-of-concept Spring Boot application for analyzing log files, identifying suspicious incidents, and preparing AI-assisted root cause investigation.

## Local development setup

### 1. Start PostgreSQL with Docker Compose

From the project root:

```bash
docker compose up -d postgres
```

This starts a local PostgreSQL instance on port 5432.

### 2. Configure environment variables

You can either export variables in your shell or create a local `.env` file.

Example:

```bash
export DB_NAME=intelliops
export DB_USERNAME=intelliops
export DB_PASSWORD=intelliops
export DB_URL=jdbc:postgresql://localhost:5432/intelliops
export SERVER_PORT=8080
```

A sample file is included at `.env.example`.

### 3. Run the application

```bash
./gradlew clean test
./gradlew bootRun
```

The application starts on:

```text
http://localhost:8080
```

### 4. Check the health endpoint

```bash
curl http://localhost:8080/api/health
```

Expected response:

```json
{"status":"UP"}
```

## Project setup notes

- Java 21
- Spring Boot 3.3.x
- PostgreSQL 16
- Gradle
- JPA + Hibernate
- Spring MVC with DTO-first design
- Environment-variable-based configuration

## Configuration values

The application expects the following configuration values:

- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- `DB_NAME`
- `SERVER_PORT`

If these are not set, sensible defaults are used from `src/main/resources/application.yaml`.

## Notes

This Phase 1 implementation provides the foundation for the project:

- Gradle build configuration
- PostgreSQL datasource setup
- Docker Compose for local database support
- JPA entities and repositories for incidents and log entries
- health endpoint and global exception handling
- validation-friendly DTOs and structured responses

AI integration and log analysis workflows are intentionally not implemented yet.

