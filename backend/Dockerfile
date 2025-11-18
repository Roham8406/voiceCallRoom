FROM maven:3.9.2-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:17
WORKDIR /app
COPY --from=build /app/target/voicechat-backend-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
CMD ["java","-jar","/app/app.jar"]
