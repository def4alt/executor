FROM gradle:8.14.4-jdk21 AS build
WORKDIR /workspace
COPY . .
RUN gradle --no-daemon bootJar

FROM eclipse-temurin:21-jre
RUN useradd --system --create-home --uid 10001 executor
WORKDIR /app
COPY --from=build /workspace/build/libs/*.jar app.jar
RUN chown -R executor:executor /app
USER 10001
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
