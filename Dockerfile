# ─────────────────────────────────────────────────────────────────────────────
# STAGE 1: BUILD
# We use the full Maven + JDK image here to compile the project.
# This stage won't be included in the final image — it's just for building.
# ─────────────────────────────────────────────────────────────────────────────
FROM maven:3.9.6-eclipse-temurin-17 AS builder

WORKDIR /app

# Copy pom.xml first and download dependencies.
# Docker caches each RUN layer — if pom.xml hasn't changed, this layer
# is cached and Maven won't re-download the internet every build.
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Now copy source code and build
COPY src ./src
RUN mvn package -DskipTests -B

# ─────────────────────────────────────────────────────────────────────────────
# STAGE 2: RUNTIME
# We use a slim JRE-only image (~100 MB vs ~500 MB for full JDK).
# Only the compiled JAR is copied from stage 1.
# ─────────────────────────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Create a non-root user for security — never run apps as root in containers
RUN addgroup -S metavault && adduser -S metavault -G metavault

# Copy the fat JAR from the builder stage
COPY --from=builder /app/target/metavault-*.jar app.jar

# Change ownership of app files to the non-root user
RUN chown metavault:metavault app.jar

USER metavault

EXPOSE 8080

# JVM flags:
# -XX:+UseContainerSupport: respect Docker CPU/memory limits
# -XX:MaxRAMPercentage=75.0: use up to 75% of container memory for heap
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
