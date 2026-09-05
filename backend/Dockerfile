FROM eclipse-temurin:25-jdk AS build
WORKDIR /app
COPY gradlew gradlew.bat settings.gradle.kts build.gradle.kts ./
COPY gradle ./gradle
RUN chmod +x gradlew
COPY src/main ./src/main
RUN --mount=type=cache,target=/root/.gradle ./gradlew --no-daemon bootJar

FROM eclipse-temurin:25-jre
WORKDIR /app
RUN groupadd --system organization && useradd --system --gid organization organization
COPY --from=build /app/build/libs/organization-api-0.1.0.jar /app/application.jar
USER organization
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/application.jar"]
