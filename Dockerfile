FROM openjdk:17-jdk-slim

ARG JAR_FILE=build/libs/*.jar
ARG BUILD_DATE
COPY ${JAR_FILE} app.jar

ENTRYPOINT ["java", "-jar", "/app.jar"]

EXPOSE 8080
