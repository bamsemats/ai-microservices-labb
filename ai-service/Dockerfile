# Build stage
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

# Copy poms to satisfy Maven's monorepo validation
COPY pom.xml .
COPY proto/pom.xml proto/
COPY common-security/pom.xml common-security/
COPY common-observability/pom.xml common-observability/
COPY gateway/pom.xml gateway/
COPY auth-service/pom.xml auth-service/
COPY user-service/pom.xml user-service/
COPY message-service/pom.xml message-service/
COPY ai-service/pom.xml ai-service/
COPY content-aggregator-service/pom.xml content-aggregator-service/

# Install parent and shared modules
RUN --mount=type=cache,target=/root/.m2 mvn install -N -B -Dmaven.repo.local=/root/.m2
COPY proto/src proto/src
COPY common-security/src common-security/src
COPY common-observability/src common-observability/src
RUN --mount=type=cache,target=/root/.m2 mvn install -pl proto,common-security,common-observability -Dmaven.test.skip=true -B -Dmaven.repo.local=/root/.m2

# Build the service
COPY ai-service/src ai-service/src
RUN --mount=type=cache,target=/root/.m2 mvn package -pl ai-service -Dmaven.test.skip=true -B -Dmaven.repo.local=/root/.m2

# Run stage
FROM eclipse-temurin:21-jre
RUN groupadd -g 1000 appgroup && useradd -u 1000 -g appgroup -s /bin/sh appuser
WORKDIR /app
COPY --from=build /app/ai-service/target/*.jar app.jar
RUN chown -R appuser:appgroup /app
USER appuser
EXPOSE 8084
ENTRYPOINT ["java", "-jar", "app.jar"]
