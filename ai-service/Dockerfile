FROM eclipse-temurin:21-jre
WORKDIR /app
COPY ai-service/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
