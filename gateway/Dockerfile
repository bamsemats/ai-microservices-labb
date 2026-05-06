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
RUN mvn install -N -B
COPY common-observability/src common-observability/src
COPY common-observability/src common-observability/src
RUN mvn install -pl common-observability -DskipTests -B

# Build the service
COPY gateway/src gateway/src
RUN mvn package -pl gateway -DskipTests -B

# Run stage
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/gateway/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
