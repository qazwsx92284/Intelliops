# IntelliOps – AI-Powered Incident Analyzer

An AI-assisted incident investigation platform built with Java and Spring Boot.

IntelliOps is a personal proof-of-concept project that explores how AI can help software engineers investigate application errors, identify suspicious log patterns, and accelerate initial incident troubleshooting.

The project is inspired by real-world production support experiences, where engineers often need to manually review large volumes of application logs to understand what went wrong.

## Project Objectives

The goal of IntelliOps is to reduce the manual effort involved in initial incident investigation by combining traditional log processing techniques with AI-powered analysis.

The planned workflow is:

1. Accept application logs through file uploads or text input.
2. Parse and normalize log entries into a structured format.
3. Detect suspicious patterns using rule-based analysis.
4. Group related errors into potential incidents.
5. Use an AI model to investigate selected log context.
6. Generate suspected root causes, supporting evidence, potential impact, and troubleshooting recommendations.
7. Store incident records and analysis results for future review.

AI-generated findings are intended to support engineers during investigation, not replace human verification or confirm root causes automatically.

## Technology Stack

| Category | Technology |
|---|---|
| Language | Java 21 |
| Backend | Spring Boot 3.3.x |
| Build Tool | Gradle |
| Database | PostgreSQL 16 |
| ORM | Spring Data JPA / Hibernate |
| Web | Spring MVC |
| AI Integration | Spring AI / OpenAI API (planned) |
| Containerization | Docker Compose |
| Testing | JUnit 5 / Mockito |

## Architecture

IntelliOps is designed as a single Spring Boot application with a layered architecture.

```text
                 IntelliOps
                      |
              Log Input Layer
                      |
              Log Processing
                      |
           Rule-Based Detection
                      |
             AI Analysis Layer
                      |
             Incident Management
                      |
               PostgreSQL
                      |
               Web Dashboard
```

### Core Components

- **Controller:** Handles HTTP requests and user interactions.
- **Service:** Contains application and business logic.
- **Parser:** Converts raw application logs into structured log entries.
- **Detection:** Identifies suspicious patterns and potential incidents.
- **AI:** Analyzes selected incident context and generates investigation assistance.
- **Repository:** Handles database operations.
- **Entity / DTO:** Defines persistence models and API data structures.
- **Configuration:** Manages application and infrastructure settings.

## Current Implementation Status

### Phase 1 – Application Foundation

**Status: Implemented**

The initial foundation of the application includes:

- Gradle-based Spring Boot project configuration.
- PostgreSQL datasource configuration.
- Docker Compose configuration for local database development.
- JPA entities and repositories for incidents and log entries.
- DTO-based request and response structure.
- Health check endpoint.
- Global exception handling.
- Environment-variable-based configuration.

### Planned Development

| Phase | Features | Status |
|---|---|---|
| 1 | Spring Boot foundation and database setup | Implemented |
| 2 | Log parsing and normalization | Planned |
| 3 | Rule-based incident detection | Planned |
| 4 | Spring AI integration and incident analysis | Planned |
| 5 | Incident dashboard and history | Planned |

## Local Development Setup

### Prerequisites

- Java 21
- Docker Desktop
- Gradle Wrapper (included in the project)
- PostgreSQL 16 (provided through Docker Compose)

### 1. Clone the Repository

```bash
git clone <YOUR_GITHUB_REPOSITORY_URL>
cd intelliops
```

### 2. Start PostgreSQL

From the project root:

```bash
docker compose up -d postgres
```

Verify that the database container is running:

```bash
docker compose ps
```

### 3. Configure Environment Variables

The application supports the following environment variables:

| Variable | Default / Example |
|---|---|
| DB_NAME | intelliops |
| DB_USERNAME | intelliops |
| DB_PASSWORD | intelliops |
| DB_URL | jdbc:postgresql://localhost:5432/intelliops |
| SERVER_PORT | 8080 |

A sample configuration is provided in `.env.example`.

For local development, configure these values in your shell before starting the Spring Boot application.

Example using PowerShell:

```powershell
$env:DB_NAME="intelliops"
$env:DB_USERNAME="intelliops"
$env:DB_PASSWORD="intelliops"
$env:DB_URL="jdbc:postgresql://localhost:5432/intelliops"
$env:SERVER_PORT="8080"
```

Note: Docker Compose can read values from a project-root `.env` file. Spring Boot does not automatically load a plain `.env` file unless additional support is configured. Export the variables into your shell when running the application locally.

### 4. Build and Test

On macOS/Linux:

```bash
./gradlew clean test
```

On Windows PowerShell:

```powershell
.\gradlew clean test
```

### 5. Run the Application

On macOS/Linux:

```bash
./gradlew bootRun
```

On Windows PowerShell:

```powershell
.\gradlew bootRun
```

The application starts at:

http://localhost:8080

### 6. Health Check

Send a GET request to:

```text
http://localhost:8080/api/health
```

Expected response:

```json
{
  "status": "UP"
}
```

## Database Configuration

PostgreSQL runs locally in a Docker container.

The application connects to the database using Spring Data JPA and Hibernate.

The initial persistence layer includes entities and repositories for:

- Incident
- LogEntry

The database configuration is managed through environment variables and application configuration files.

## Security and Configuration

- Database credentials are provided through environment variables.
- API credentials for future AI integration will also be managed through environment variables.
- Secrets and local configuration files must not be committed to Git.
- Demonstration data will use synthetic application logs rather than real company or customer information.

## Project Scope and Limitations

IntelliOps is currently a personal proof-of-concept project under development.

The initial implementation establishes the application and database foundation. Log parsing, rule-based detection, AI-powered analysis, and the dashboard are planned development phases and should not be considered completed until implemented and tested.

The project is not intended to replace enterprise monitoring platforms or automatically determine confirmed root causes.

## Future Improvements

Potential extensions include:

- Integration with monitoring and log aggregation platforms such as Datadog or Elasticsearch.
- More advanced incident correlation.
- Historical incident retrieval and knowledge-based analysis.
- Email notifications for high-priority incidents.
- Improved AI evaluation and analysis reliability.

---

**Project:** IntelliOps – AI-Powered Incident Analyzer  
**Type:** Personal Proof of Concept  
**Backend:** Java / Spring Boot  
**Build Tool:** Gradle
