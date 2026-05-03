# Build stage
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

# Copy poms to satisfy Maven's monorepo validation
COPY pom.xml .
COPY proto/pom.xml proto/
COPY common-security/pom.xml common-security/
COPY gateway/pom.xml gateway/
COPY auth-service/pom.xml auth-service/
COPY user-service/pom.xml user-service/
COPY message-service/pom.xml message-service/
COPY ai-service/pom.xml ai-service/
COPY content-aggregator-service/pom.xml content-aggregator-service/

# Install parent and shared modules
RUN mvn install -N -B
COPY proto/src proto/src
COPY common-security/src common-security/src
RUN mvn install -pl proto,common-security -DskipTests -B

# Build the service
COPY ai-service/src ai-service/src
RUN mvn package -pl ai-service -DskipTests -B

# Run stage
FROM eclipse-temurin:21-jre
RUN groupadd -r appgroup && useradd -r -g appgroup appuser
WORKDIR /app
COPY --from=build --chown=appuser:appgroup /app/ai-service/target/*.jar app.jar
USER appuser
EXPOSE 8084
ENTRYPOINT ["java", "-jar", "app.jar"]
