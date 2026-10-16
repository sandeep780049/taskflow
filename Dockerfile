# --- Stage 1: build the Spring Boot jar ---
# The frontend is pre-built and committed to src/main/resources/static,
# so no Node.js/npm is needed in this build.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Cache dependencies: copy pom first, resolve, then copy sources
COPY pom.xml .
RUN mvn -B -q dependency:go-offline || mvn -B -q dependency:resolve

COPY src ./src
RUN mvn -B package -DskipTests

# --- Stage 2: minimal runtime ---
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S taskflow && adduser -S taskflow -G taskflow
COPY --from=build /app/target/taskflow-api.jar app.jar
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -Xms128m -Xmx512m"
USER taskflow
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
