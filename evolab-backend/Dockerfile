FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# The jar is built outside Docker with Gradle and copied into the runtime image.
COPY app/build/libs/app-*-SNAPSHOT.jar app.jar

ENV SPRING_PROFILES_ACTIVE=default
ENV DOCKER_HOST=unix:///var/run/docker.sock

ENTRYPOINT ["java", "-jar", "app.jar"]
