# syntax=docker/dockerfile:1
# =====================================================================
# backend_gestion_loyers — Spring Boot (Java 21) — port 8083
# =====================================================================

# ---------- Stage 1 : build (Maven -> jar exécutable) ----------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

COPY pom.xml ./
COPY src ./src

# Le cache BuildKit évite de retélécharger les dépendances Maven à chaque build
RUN --mount=type=cache,target=/root/.m2 \
    mvn -q -B package -DskipTests \
 && cp target/*.jar /app/app.jar

# ---------- Stage 2 : runtime (JRE uniquement) ----------
FROM eclipse-temurin:21-jre-alpine AS runtime
ENV SERVER_PORT=8083
WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring
COPY --from=build --chown=spring:spring /app/app.jar ./app.jar

USER spring

EXPOSE 8083
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]

