FROM openjdk:8-jdk-alpine
ARG JAR_FILE=build/libs/save-orchestrator*.jar
COPY ${JAR_FILE} save-orchestrator.jar
ENTRYPOINT ["java","-jar","/save-orchestrator.jar"]