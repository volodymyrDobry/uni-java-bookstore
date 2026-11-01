# Build the book-management-service bootable jar (multi-module build; context is the repo root).
FROM eclipse-temurin:25-jdk AS build
WORKDIR /workspace
COPY . .
RUN chmod +x gradlew && ./gradlew :book-management-service:bootJar --no-daemon -x test

FROM eclipse-temurin:25-jre
WORKDIR /app
# Build-time version, surfaced at runtime via /actuator/info (info.app.version).
ARG APP_VERSION=dev
ENV APP_VERSION=${APP_VERSION}
COPY --from=build /workspace/book-management-service/build/libs/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
