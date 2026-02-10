FROM openjdk:17-jdk-slim

WORKDIR /app

COPY target/teamup-server-1.0.0.jar app.jar

EXPOSE 8080 9092

ENTRYPOINT ["java", "-jar", "app.jar"]
