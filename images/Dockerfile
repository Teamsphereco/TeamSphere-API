FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /app

COPY pom.xml .

COPY backend-service/pom.xml backend-service/

COPY backend-service/checkstyle.xml backend-service/

COPY backend-service/src backend-service/src

RUN mvn clean package -pl backend-service -am -DskipTests

FROM eclipse-temurin:21-jdk-alpine

ENV JAR_VERSION=teamsphere-backend

COPY --from=builder /app/backend-service/target/*.jar ${JAR_VERSION}.jar

ENTRYPOINT ["sh", "-c", "java  -jar ${JAR_VERSION}.jar"]
