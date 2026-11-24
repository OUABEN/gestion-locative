# syntax=docker/dockerfile:1
# ---------- Étape 1 : compilation (le cache Maven est conservé entre les builds) ----------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn -q -B -DskipTests package

# ---------- Étape 2 : image d'exécution légère ----------
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN addgroup -S app && adduser -S app -G app
COPY --from=build /app/target/*.jar app.jar
USER app
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "app.jar"]
