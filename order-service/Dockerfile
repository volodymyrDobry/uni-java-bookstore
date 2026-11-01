# Build the order-service bootable jar (multi-module build; context is the repo root).
# Build on the native runner arch (the jar is arch-independent); only the
# runtime base below varies per target platform — fast multi-arch, no emulation.
FROM --platform=$BUILDPLATFORM eclipse-temurin:25-jdk AS build
WORKDIR /workspace
COPY . .
RUN chmod +x gradlew && ./gradlew :order-service:bootJar --no-daemon -x test

FROM eclipse-temurin:25-jre
WORKDIR /app
# Build-time version, surfaced at runtime via /actuator/info (info.app.version).
ARG APP_VERSION=dev
ENV APP_VERSION=${APP_VERSION}
COPY --from=build /workspace/order-service/build/libs/*.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "app.jar"]
