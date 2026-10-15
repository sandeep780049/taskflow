# --- Stage 1: build the Spring Boot jar ---
FROM maven:3.9-eclipse-temurin-21 AS backend-build
WORKDIR /app/backend

# separate dependency download from compilation so retries are targeted
COPY pom.xml .
RUN mvn -B --no-transfer-progress dependency:resolve

COPY src ./src
RUN mvn -B --no-transfer-progress package -DskipTests

# --- Stage 2: runtime container ---
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S taskflow && adduser -S taskflow -G taskflow

COPY --from=backend-build /app/backend/target/taskflow-api.jar app.jar

ENV JAVA_OPTS="-XX:+UseZGC -XX:MaxRAMPercentage=75.0 -Xms128m -Xmx512m"

USER taskflow
EXPOSE 8080

HEALTHCHECK --interval=10s --timeout=3s --retries=3 --start-period=10s \
  CMD wget -qO- http://localhost:8080/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]


