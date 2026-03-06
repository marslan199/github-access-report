# GitHub Access Report Service

A production-quality Spring Boot service that connects to the GitHub API and generates a report showing which users have access to which repositories within a GitHub organization.

## Prerequisites

- Java 21+
- Maven 3.8+
- A GitHub Personal Access Token with `read:org` and `repo` scopes

## Configuration

The service reads the GitHub token from an environment variable. Set it before running:

**Windows (PowerShell):**
```powershell
$env:GITHUB_TOKEN = "ghp_your_token_here"
```

**Linux / macOS:**
```bash
export GITHUB_TOKEN=ghp_your_token_here
```

The `application.yml` binds this value automatically:
```yaml
github:
  api:
    token: ${GITHUB_TOKEN}
```

## Running the Project

```bash
mvn spring-boot:run
```

The server starts on port **8080** by default.

## API Usage

### Endpoint

```
GET /api/access-report?org={organization}
```

| Parameter | Type   | Required | Description                     |
|-----------|--------|----------|---------------------------------|
| `org`     | String | Yes      | GitHub organization login name  |

### Example Request

```bash
curl "http://localhost:8080/api/access-report?org=my-org"
```

### Example Response

```json
{
  "organization": "my-org",
  "users": [
    {
      "username": "alice",
      "repositories": ["backend-api", "frontend-app"]
    },
    {
      "username": "bob",
      "repositories": ["backend-api"]
    }
  ]
}
```

### Error Responses

All errors return RFC 7807 `ProblemDetail` JSON:

| Scenario                 | HTTP Status |
|--------------------------|-------------|
| Organization not found   | `404`       |
| Invalid/missing token    | `401`       |
| GitHub API error         | Mirrors GitHub's status |
| Blank `org` parameter    | `400`       |

## Architecture & Design Decisions

### Reactive Pipeline with Bounded Concurrency

The core of the service is a reactive Flux pipeline in `AccessReportService`:

```
fetchOrgRepositories(org)
   └─ flatMap(repo -> fetchCollaborators(repo), concurrency=20)
         └─ aggregator.addAccess(user, repo)
```

`flatMap` with a concurrency limit of **20** allows up to 20 collaborator API calls to run in parallel at any time, making the service fast for large orgs (100+ repos, 1000+ users) while avoiding GitHub rate-limit exhaustion.

### Pagination

`GithubApiClient.paginateFlux()` is a generic helper that keeps fetching pages until GitHub returns an empty page — matching GitHub's documented pagination termination behavior. `per_page` is configurable in `application.yml` (default: 100, the GitHub maximum).

### Thread-Safe Aggregation

`ConcurrentAggregator` uses a `ConcurrentHashMap` with `computeIfAbsent` and a `Collections.synchronizedList` per user to safely collect results written by multiple concurrent Reactor threads.

### Error Handling

- `GlobalExceptionHandler` maps domain exceptions and `WebClientResponseException` to structured RFC 7807 `ProblemDetail` responses.
- `403 Forbidden` and `404 Not Found` on collaborator endpoints are silently skipped (e.g., private repos the token can't inspect), so the report is always returned rather than failing.

### Tech Stack

| Component         | Technology             |
|-------------------|------------------------|
| Framework         | Spring Boot 3.4.3      |
| Java Version      | Java 21                |
| HTTP Client       | Spring WebFlux WebClient |
| JSON              | Jackson (via Spring)   |
| Build             | Maven                  |

## Project Structure

```
src/main/java/com/example/githubaccess/
├── GithubAccessApplication.java
├── config/
│   └── GithubConfig.java           # WebClient bean with auth headers
├── controller/
│   └── AccessReportController.java # REST endpoint
├── service/
│   └── AccessReportService.java    # Orchestration & aggregation logic
├── client/
│   └── GithubApiClient.java        # GitHub API pagination & calls
├── model/
│   ├── RepositoryResponse.java     # GitHub repo DTO
│   ├── CollaboratorResponse.java   # GitHub collaborator DTO
│   ├── UserAccessReport.java       # Per-user output DTO
│   └── AccessReportResponse.java   # Top-level response DTO
├── util/
│   └── ConcurrentAggregator.java   # Thread-safe user→repo map
└── exception/
    └── GlobalExceptionHandler.java # RFC 7807 error handling
src/main/resources/
└── application.yml
```
