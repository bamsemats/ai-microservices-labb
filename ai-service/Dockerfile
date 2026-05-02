FROM openjdk:21-jdk-slim
WORKDIR /app
COPY ai-service/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
