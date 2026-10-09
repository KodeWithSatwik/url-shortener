# URL Shortener — Agentic SDLC Demonstration

A production-quality URL shortener service built using Java 17 + Spring Boot 3, developed end-to-end through an agentic SDLC workflow demonstrating requirement understanding, task decomposition, multi-step execution, and governed orchestration.

---

## Prerequisites

| Tool | Version | Notes |
|------|---------|-------|
| Java JDK | 17+ | Set `JAVA_HOME` to JDK (not JRE) |
| Maven | 3.9+ | Or use `./mvnw` (wrapper included) |

Check your setup:
```bash
java -version    # should show 17+
javac -version   # must be present (JDK, not JRE)
mvn -version     # or ./mvnw -version
```

---

## Quick Start

```bash
# 1. Clone / navigate to project
cd url-shortener

# 2. Build
mvn clean package -DskipTests

# 3. Run (uses H2 in-memory DB, no setup needed)
mvn spring-boot:run

# 4. Service is live at http://localhost:8080
```

---

## Run Tests

```bash
mvn test
```

Expected output:
```
Tests run: 36, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Coverage report generated at: `reports/coverage/index.html`

---

## API Quick Start

### Shorten a URL
```bash
curl -X POST http://localhost:8080/api/v1/urls \
  -H "Content-Type: application/json" \
  -d '{"originalUrl": "https://www.example.com/some/very/long/path"}'
```
Response:
```json
{
  "shortCode": "1",
  "shortUrl": "http://localhost:8080/1",
  "originalUrl": "https://www.example.com/some/very/long/path",
  "expiresAt": "2026-11-08T21:30:00",
  "createdAt": "2026-10-09T21:30:00"
}
```

### Redirect
```bash
curl -L http://localhost:8080/1
# Follows HTTP 302 redirect to original URL
```

### Custom Alias (Brownfield Feature)
```bash
curl -X POST http://localhost:8080/api/v1/urls \
  -H "Content-Type: application/json" \
  -d '{"originalUrl": "https://example.com", "customAlias": "my-link"}'
```

### Delete
```bash
curl -X DELETE http://localhost:8080/api/v1/urls/my-link
```

### Analytics
```bash
curl http://localhost:8080/api/v1/urls/my-link/analytics
```

### Expiry (TTL)
```bash
curl -X POST http://localhost:8080/api/v1/urls \
  -H "Content-Type: application/json" \
  -d '{"originalUrl": "https://example.com", "expiresAt": "2026-10-15T00:00:00"}'
```

---

## Swagger UI

After starting the application, visit:

```
http://localhost:8080/swagger-ui.html
```

Full interactive API documentation with request/response schemas.

---

## H2 Console (dev only)

```
http://localhost:8080/h2-console
JDBC URL: jdbc:h2:mem:urlshortenerdb
Username: sa
Password: (empty)
```

---

## Configuration

Key settings in [`src/main/resources/application.yml`](src/main/resources/application.yml):

| Property | Default | Description |
|----------|---------|-------------|
| `app.base-url` | `http://localhost:8080` | Base URL used in shortened URL responses |
| `app.default-ttl-days` | `30` | Default URL expiry in days |
| `app.short-code.max-retries` | `5` | Max collision retries |
| `app.rate-limit.capacity` | `20` | Max requests per IP |
| `app.rate-limit.refill-seconds` | `60` | Rate limit window in seconds |

---

## Project Structure

```
url-shortener/
├── SDLC_MASTER_TODO.md              ← Master SDLC task tracker
├── pom.xml
├── docs/
│   ├── requirements/                ← Functional & non-functional requirements, per-phase todos
│   ├── architecture/
│   │   ├── architecture-overview.md ← Component diagram, data flows, tech stack
│   │   ├── orchestration-design.md  ← Agentic workflow model, gates, metrics, re-planning
│   │   └── adr/                     ← ADR-001 through ADR-005
│   ├── scenarios/
│   │   ├── greenfield-scenario.md   ← Greenfield: URL shortener from scratch
│   │   ├── brownfield-scenario.md   ← Brownfield: custom alias enhancement + impact analysis
│   │   └── ambiguous-requirements.md← Ambiguous: "add user management" — scoped interpretation
│   └── decisions/
│       ├── decision-log.md          ← All engineering decisions with rationale
│       └── audit-log.md             ← Timestamped log of every stage transition & approval
├── src/
│   ├── main/java/com/urlshortener/
│   │   ├── controller/              ← UrlController, AnalyticsController
│   │   ├── service/                 ← UrlShortenerService, AnalyticsService, Base62Encoder
│   │   ├── repository/              ← ShortenedUrlRepository, ClickEventRepository
│   │   ├── model/                   ← ShortenedUrl, ClickEvent (JPA entities)
│   │   ├── dto/                     ← ShortenUrlRequest/Response, AnalyticsResponse, ErrorResponse
│   │   ├── exception/               ← Domain exceptions + GlobalExceptionHandler
│   │   └── config/                  ← AppConfig, AsyncConfig, RateLimitFilter
│   └── test/
│       ├── unit/                    ← Base62EncoderTest, UrlShortenerServiceTest
│       └── integration/             ← UrlShortenerIntegrationTest
└── reports/                         ← Coverage reports
```

---

## Architecture Summary

```
HTTP Client
    │
    ▼
UrlController / AnalyticsController  (Spring MVC, Jakarta Validation)
    │
    ▼
UrlShortenerService / AnalyticsService  (business logic, Base62 encoding)
    │
    ▼
ShortenedUrlRepository / ClickEventRepository  (Spring Data JPA)
    │
    ▼
H2 (dev) / PostgreSQL (prod)  ← schema via Flyway migrations
```

- **Rate limiting**: Bucket4j token bucket filter, per-IP, before Spring MVC
- **Analytics**: async (`@Async` thread pool) — never blocks redirect latency
- **Validation**: Jakarta Validation 3.0 (`@Pattern`, `@NotBlank`, `@Size`)
- **Error handling**: `@RestControllerAdvice` — consistent JSON error responses

---

## Docker (Recommended for Production-like Testing)

The service ships with a multi-stage `Dockerfile` and a `docker-compose.yml` that wires it to PostgreSQL 16 — no local Java or Maven installation required.

### Start everything with one command

```bash
cd url-shortener
docker compose up --build
```

This will:
1. Compile the fat JAR inside a Maven + JDK 17 build container
2. Copy the JAR into a minimal JRE-only runtime image (~220 MB)
3. Start PostgreSQL 16 and wait for it to be healthy before starting the app
4. Run Flyway migrations automatically on first boot
5. Expose the service at `http://localhost:8080`

### Useful commands

```bash
# Start in background
docker compose up -d --build

# Check both services are healthy
docker compose ps

# Follow app logs
docker compose logs -f app

# Stop and remove containers (keep DB data)
docker compose down

# Stop and wipe the database volume (fresh start)
docker compose down -v
```

### Environment variables (docker-compose.yml defaults)

| Variable | Default in `.env.example` | Description |
|----------|--------------------------|-------------|
| `DATABASE_URL` | `jdbc:postgresql://db:5432/urlshortener` | PostgreSQL JDBC URL (uses service name `db`) |
| `DATABASE_USER` | `urlshortener` | PostgreSQL username |
| `DATABASE_PASSWORD` | *(set in `.env` — never committed)* | PostgreSQL password |
| `APP_BASE_URL` | `http://localhost:8080` | Base URL used in shortened URL responses |

> **Production hardening note:** change `DATABASE_PASSWORD` and `APP_BASE_URL`, put secrets in a secrets manager (e.g. Docker Secrets, AWS Secrets Manager), and front the container with an HTTPS-terminating reverse proxy (nginx / ALB).

---

## Production Deployment (without Docker)

Set these environment variables and run with `--spring.profiles.active=prod`:

| Variable | Example |
|----------|---------|
| `DATABASE_URL` | `jdbc:postgresql://localhost:5432/urlshortener` |
| `DATABASE_USER` | `urlshortener` |
| `DATABASE_PASSWORD` | `secret` |
| `APP_BASE_URL` | `https://short.example.com` |

---

## Testing Approach

- **Unit tests**: Mock-based (`@ExtendWith(MockitoExtension.class)`) for service and encoder logic
- **Integration tests**: Full Spring context (`@SpringBootTest`) with H2 in-memory, MockMvc
- **Coverage**: JaCoCo, reports at `reports/coverage/index.html`
- **Edge cases covered**: expired URLs, deleted URLs, duplicate aliases, invalid URL schemes, rate limiting, idempotency

See [`docs/requirements/PHASE3_TODO.md`](docs/requirements/PHASE3_TODO.md) for full test inventory.

---

## Kubernetes (Production Reference Manifests)

The `k8s/` directory contains reference manifests for deploying to a Kubernetes cluster.

```
k8s/
├── configmap.yaml        # Non-sensitive config (Spring profile, DB URL, base URL)
├── secret.example.yaml   # Secret template — copy to secret.yaml, fill values, never commit
├── service.yaml          # ClusterIP Service (port 80 → container 8080)
└── deployment.yaml       # 2 replicas, rolling update, liveness/readiness probes, resource limits
```

### Deploy order

```bash
# 1. Build and push your image
docker build -t your-registry/urlshortener:1.0.0 .
docker push your-registry/urlshortener:1.0.0

# 2. Create secrets (copy template, fill real values)
cp k8s/secret.example.yaml k8s/secret.yaml
# edit k8s/secret.yaml — base64-encode your real credentials

# 3. Apply all manifests
kubectl apply -f k8s/configmap.yaml
kubectl apply -f k8s/secret.yaml
kubectl apply -f k8s/service.yaml
kubectl apply -f k8s/deployment.yaml

# 4. Verify
kubectl rollout status deployment/urlshortener
kubectl get pods -l app=urlshortener
```

Key production details baked into the manifests:
- **Readiness + liveness probes** on `/actuator/health` — pods only receive traffic when Spring is ready; unhealthy pods are restarted automatically
- **Resource limits** — 500m CPU / 512Mi RAM; aligned with `MaxRAMPercentage=75` JVM flag (~384Mi heap)
- **Rolling update with `maxUnavailable: 0`** — zero-downtime deploys
- **Pod anti-affinity** — two replicas prefer different nodes
- **`preStop` sleep** — gives the load balancer time to drain connections before shutdown

---

## CI Pipeline

Every push and pull request to `main`/`master` triggers the GitHub Actions workflow at [`.github/workflows/ci.yml`](.github/workflows/ci.yml):

1. Checks out source
2. Sets up JDK 17 (Temurin) with Maven dependency cache
3. Runs `mvn clean verify` (compile + all 36 tests + JaCoCo coverage)
4. Uploads the coverage report as a build artifact (retained 14 days)

---

## Agentic Orchestration

This project was built end-to-end by an AI agent under human oversight. The orchestration model — dependency graph, human approval gates, fault tolerance, audit log, and reliability metrics — is documented at:

- [`docs/architecture/orchestration-design.md`](docs/architecture/orchestration-design.md)
- [`docs/decisions/audit-log.md`](docs/decisions/audit-log.md)
- [`docs/scenarios/`](docs/scenarios/) — greenfield, brownfield, and ambiguous requirement scenarios

See [`docs/FINAL_ENGINEERING_SUMMARY.md`](docs/FINAL_ENGINEERING_SUMMARY.md) for the complete project summary.
