# Docker build for local / docker-compose usage.
# Render uses the buildpack approach in render.yaml (buildCommand + startCommand),
# so this Dockerfile is not required for Render deploys.
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY target/taskflow-api.jar app.jar
ENV JAVA_OPTS="-XX:+UseZGC -XX:MaxRAMPercentage=75.0 -Xms128m -Xmx512m"
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]


