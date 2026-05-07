FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Assume the jar is built outside and placed in the build/libs directory
COPY app/build/libs/app-*-SNAPSHOT.jar app.jar

ENV SPRING_PROFILES_ACTIVE=default
ENV DOCKER_HOST=unix:///var/run/docker.sock

ENTRYPOINT ["java", "-jar", "app.jar"]
