# BUILD

FROM maven:3.9-eclipse-temurin-17-alpine AS builder
WORKDIR /app
COPY pom.xml ./
RUN mvn dependency:go-offline
COPY src/ src/
RUN mvn package -DskipTests

# DEPLOY

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=builder /app/target/matching-*-SNAPSHOT.jar app.jar
EXPOSE 8080
CMD java -jar app.jar