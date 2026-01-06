# Build stage
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY pom.xml .
COPY src ./src
COPY db ./db
RUN mvn -B -DskipTests clean package

# Runtime stage
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=build /workspace/target/MediaRatingPlatform.jar /app/app.jar
COPY --from=build /workspace/db /app/db
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]