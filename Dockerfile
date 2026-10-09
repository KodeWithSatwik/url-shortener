# ──────────────────────────────────────────────────────────────────────────────
# Stage 1: Build
# Uses the official Maven + JDK 17 image to compile and package the fat JAR.
# The target/ directory and Maven local repo are kept in this stage only.
# ──────────────────────────────────────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /app

# Copy dependency manifest first — Docker cache layer: only re-downloads deps
# when pom.xml changes, not on every source code change.
COPY pom.xml ./
RUN mvn dependency:go-offline -q

# Copy source and build the fat JAR (skip tests — tests run in CI, not in image build)
COPY src ./src
RUN mvn clean package -DskipTests -q

# ──────────────────────────────────────────────────────────────────────────────
# Stage 2: Runtime
# Minimal JRE-only image. No compiler, no Maven, no source code.
# Eclipse Temurin 17 JRE on Debian Bookworm slim (~220 MB total image).
# ──────────────────────────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jre AS runtime

# Non-root user for security — never run as root in production containers
RUN groupadd --system appgroup && useradd --system --gid appgroup appuser

WORKDIR /app

# Copy only the fat JAR from the build stage
COPY --from=build /app/target/url-shortener-*.jar app.jar

# Switch to non-root user
USER appuser

# Expose the Spring Boot default port
EXPOSE 8080

# Health check — Docker/Compose will mark the container healthy once Actuator responds
HEALTHCHECK --interval=30s --timeout=5s --start-period=45s --retries=3 \
  CMD wget -qO- http://localhost:8080/actuator/health || exit 1

# JVM flags:
#   -XX:+UseContainerSupport        respect container CPU/memory limits (default in JDK 11+)
#   -XX:MaxRAMPercentage=75.0       use up to 75% of container RAM for heap
#   -Djava.security.egd=...         faster entropy for UUID generation (analytics)
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
