# Architecture Overview — URL Shortener Service

**Version**: 1.0
**Phase**: Greenfield (Phase 1)

---

## System Components

```
┌─────────────────────────────────────────────────────────────────┐
│                        HTTP Client                              │
└───────────────────────────┬─────────────────────────────────────┘
                            │ HTTP
┌───────────────────────────▼─────────────────────────────────────┐
│                   Spring Boot 3 Application                     │
│                                                                 │
│  ┌──────────────────┐   ┌──────────────────┐                   │
│  │  UrlController   │   │AnalyticsController│                   │
│  │  POST /api/v1/   │   │GET /api/v1/urls/  │                   │
│  │  urls            │   │{code}/analytics   │                   │
│  │  GET /{code}     │   └────────┬─────────┘                   │
│  │  DELETE /api/v1/ │            │                              │
│  │  urls/{code}     │            │                              │
│  └────────┬─────────┘            │                              │
│           │                      │                              │
│  ┌────────▼──────────────────────▼──────────┐                  │
│  │          UrlShortenerService              │                  │
│  │  shorten() / resolve() / delete()         │                  │
│  └────────────────┬──────────────────────────┘                  │
│                   │  async                                       │
│  ┌────────────────▼──────────────┐                              │
│  │      AnalyticsService         │                              │
│  │  recordClick() / getStats()   │                              │
│  └────────────────┬──────────────┘                              │
│                   │                                              │
│  ┌────────────────▼──────────────┐                              │
│  │  ShortenedUrlRepository       │                              │
│  │  ClickEventRepository         │  (Spring Data JPA)           │
│  └────────────────┬──────────────┘                              │
│                   │  Jakarta Persistence                         │
│  ┌────────────────▼──────────────┐                              │
│  │  H2 (dev) / PostgreSQL (prod) │  ← Flyway migrations         │
│  └───────────────────────────────┘                              │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

## Technology Stack

| Layer | Technology | Rationale |
|-------|-----------|-----------|
| Runtime | Java 21 (LTS) | Virtual threads, records, pattern matching |
| Framework | Spring Boot 3.2 | Jakarta EE 10, native image support, mature ecosystem |
| Persistence | Spring Data JPA + Hibernate 6 | Jakarta Persistence, productive repository abstraction |
| Validation | Jakarta Validation 3.0 | Declarative, integrated with Spring MVC |
| Database (dev) | H2 in-memory | Zero setup, fast test cycles, Flyway compatible |
| Database (prod) | PostgreSQL 16 | ACID, proven, horizontal read scaling via read replicas |
| Migrations | Flyway | Version-controlled schema, rollback awareness |
| API Docs | SpringDoc OpenAPI 2.x | Auto-generates from annotations, Swagger UI built-in |
| Rate Limiting | Bucket4j | Token bucket, in-memory, no external dependency |
| Build | Maven 3.9 + mvnw | Reproducible builds, wrapper avoids local version mismatch |
| Coverage | JaCoCo | Standard Java coverage, integrates with Maven |

---

## Data Flow: URL Shortening (POST /api/v1/urls)

```
Client → POST /api/v1/urls { originalUrl }
       → UrlController.shorten()
       → Jakarta Validation (URL format, length)
       → UrlShortenerService.shorten()
         → Check: does originalUrl already exist? → return existing shortCode
         → Generate shortCode: Base62(autoIncrement ID or random)
         → Collision check (existsByShortCode) → retry up to 5x if collision
         → Persist ShortenedUrl entity (shortCode, originalUrl, createdAt, expiresAt)
       → Return ShortenUrlResponse { shortCode, shortUrl, originalUrl, expiresAt }
```

---

## Data Flow: URL Redirect (GET /{shortCode})

```
Client → GET /{shortCode}
       → UrlController.redirect()
       → UrlShortenerService.resolve(shortCode)
         → Lookup ShortenedUrl by shortCode → 404 if not found
         → Check expiresAt → 410 Gone if expired
         → Return originalUrl
       → [Async] AnalyticsService.recordClick(shortCode, referrer, ip)
         → Persist ClickEvent (shortCode, timestamp, referrer, ip)
       → HTTP 302 Location: originalUrl
```

---

## Key Design Decisions (See ADRs for detail)

| Decision | Choice | ADR |
|----------|--------|-----|
| Short code strategy | Base62 encoding of auto-increment ID | ADR-001 |
| Database | H2 (dev) / PostgreSQL (prod) | ADR-002 |
| Analytics storage | Same database, separate table | ADR-003 |
| Redirect status | HTTP 302 (not 301) | ADR-004 |
| Rate limiting | Bucket4j in-memory per-IP | ADR-005 |
