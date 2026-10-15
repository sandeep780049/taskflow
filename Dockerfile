# --- Stage 1: build the Spring Boot jar (frontend pre-built and committed) ---
FROM maven:3.9-eclipse-temurin-21 AS backend-build
WORKDIR /app/backend
COPY pom.xml .
RUN mvn -B -q dependency:resolve
COPY src ./src
RUN mvn -B -q package -DskipTests

# --- Stage 2: runtime ---
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S taskflow && adduser -S taskflow -G taskflow
COPY --from=backend-build /app/backend/target/taskflow-api.jar app.jar
ENV JAVA_OPTS="-XX:+UseZGC -XX:MaxRAMPercentage=75.0"
USER taskflow
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]


