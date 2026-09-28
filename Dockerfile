# ================================
# Stage 1: Build
# ================================
FROM eclipse-temurin:25-jdk-alpine AS builder

WORKDIR /app

# Copy Gradle wrapper and config files first (layer caching)
COPY gradlew .
COPY gradle/ gradle/
COPY build.gradle .
COPY settings.gradle .

# Make gradlew executable and pre-download dependencies
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon

# Copy source and build the JAR
COPY src/ src/
RUN ./gradlew bootJar --no-daemon -x test

# ================================
# Stage 2: Runtime
# ================================
FROM eclipse-temurin:25-jre-alpine AS runtime

WORKDIR /app

# --- Environment variables (override at runtime via -e or docker-compose) ---
ENV SERVER_PORT=8080 \
    SPRING_PROFILES_ACTIVE=prod \
    JAVA_OPTS="-Xms256m -Xmx512m"

# Copy only the built JAR from the builder stage
COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE ${SERVER_PORT}

# Health check — hits Spring Boot Actuator's /actuator/health endpoint
HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
    CMD wget -qO- http://localhost:${SERVER_PORT}/actuator/health | grep -q '"status":"UP"' || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]