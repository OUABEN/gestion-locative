# syntax=docker/dockerfile:1
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

COPY pom.xml ./
COPY src ./src

RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -DskipTests package

FROM eclipse-temurin:21-jre-jammy AS runtime
ENV SERVER_PORT=8084
WORKDIR /app

RUN groupadd --gid 10001 darloc \
    && useradd --uid 10001 --gid darloc --create-home darloc \
    && mkdir -p /app/storage/quittances \
    && chown -R darloc:darloc /app

COPY --from=build --chown=darloc:darloc /app/target/service-quittances-0.0.1-SNAPSHOT.jar /app/application.jar

USER darloc
EXPOSE 8084
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/application.jar"]
