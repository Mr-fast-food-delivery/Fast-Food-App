# ---------- BUILD STAGE ----------
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app

COPY pom.xml .
COPY src ./src

RUN mvn clean package -DskipTests

# ---------- RUN STAGE ----------
FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=build /app/target/FoodApp-0.0.1-SNAPSHOT.jar app.jar
# Ensure runtime environment variables (DB, mail, AWS, etc.) are available
# for Spring's `spring.config.import=optional:file:.env`.
COPY .env .env

EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]
