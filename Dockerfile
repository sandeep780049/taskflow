# --- Stage 1: build the React dashboard ---
FROM node:22-alpine AS frontend-build
WORKDIR /app/frontend
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci --no-audit --no-fund
COPY frontend/ .
RUN npm run build

# --- Stage 2: build the Spring Boot jar ---
FROM maven:3.9-eclipse-temurin-21 AS backend-build
WORKDIR /app/backend
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
# bake the dashboard built in stage 1 into the jar
COPY --from=frontend-build /app/src/main/resources/static src/main/resources/static
RUN mvn -B -q package -DskipTests

# --- Stage 3: runtime with dashboard baked in ---
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S taskflow && adduser -S taskflow -G taskflow
COPY --from=backend-build /app/backend/target/taskflow-api.jar app.jar
ENV JAVA_OPTS=""
USER taskflow
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s --retries=3 \
  CMD wget -qO- http://localhost:8080/actuator/health | grep -q UP || exit 1
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
