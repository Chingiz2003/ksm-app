FROM gradle:8.14.3-jdk21 AS build

WORKDIR /app
COPY --chown=gradle:gradle backend ./backend
COPY --chown=gradle:gradle data ./data

WORKDIR /app/backend
RUN gradle bootJar --no-daemon

FROM eclipse-temurin:21-jre

WORKDIR /app/backend
COPY --from=build /app/backend/build/libs/*.jar app.jar
COPY --from=build /app/data /app/data

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
