FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /workspace

COPY . .
RUN chmod +x ./gradlew
RUN ./gradlew :evolab-backend:app:bootJar -x test --no-daemon

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

COPY --from=builder /workspace/evolab-backend/app/build/libs/app-*-SNAPSHOT.jar app.jar

ENV SPRING_PROFILES_ACTIVE=default
ENV DOCKER_HOST=unix:///var/run/docker.sock

ENTRYPOINT ["java", "-jar", "app.jar"]
