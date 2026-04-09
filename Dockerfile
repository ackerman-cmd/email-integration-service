FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY build/libs/email-integration-service-0.0.1-SNAPSHOT-plain.jar app.jar
EXPOSE 8084
ENTRYPOINT ["java", "-jar", "app.jar"]
