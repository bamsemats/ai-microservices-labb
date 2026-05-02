FROM eclipse-temurin:21-jre
RUN groupadd -r appgroup && useradd -r -g appgroup appuser
WORKDIR /app
COPY --chown=appuser:appgroup ai-service/target/*.jar app.jar
USER appuser
ENTRYPOINT ["java", "-jar", "app.jar"]
