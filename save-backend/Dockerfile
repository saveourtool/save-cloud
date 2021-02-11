FROM openjdk:8-jdk-alpine
ARG JAR_FILE=build/libs/save-backend*.jar
COPY ${JAR_FILE} save-backend.jar
ENTRYPOINT ["java","-jar","/save-backend.jar"]