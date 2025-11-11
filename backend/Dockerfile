# ---------- BUILD STAGE ----------
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

COPY . .
RUN mvn -q -DskipTests package

# ---------- RUN STAGE ----------
FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=build /app/target/*.jar app.jar
EXPOSE 8091
# ENTRYPOINT ["java","-jar","app.jar","--spring.config.location=classpath:/application-test.properties"]
ENTRYPOINT ["sh", "-c", "sleep 15 && java -jar app.jar --spring.config.location=classpath:/application-test.properties"]
