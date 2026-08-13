# Build stage
FROM gradle:8.8.0-jdk21 AS build
WORKDIR /app
COPY --chown=gradle:gradle . .
RUN gradle bootJar --no-daemon

# Run stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
EXPOSE 8041
ENTRYPOINT ["java", "-jar", "app.jar"]
