# Stage 1: Build the Spring Boot application using Maven and JDK 21
FROM maven:3.9.9-eclipse-temurin-21-alpine AS builder
WORKDIR /app
ENV MAVEN_OPTS="-Xmx384m -XX:+UseG1GC"
COPY lovable-clone/pom.xml .
COPY lovable-clone/src ./src
RUN mvn clean package -DskipTests -B

# Stage 2: Minimal runtime image
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar

ENV PORT=8080
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
