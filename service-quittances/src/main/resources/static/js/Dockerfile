FROM eclipse-temurin:21-jre-jammy

RUN groupadd --gid 10001 darloc \
    && useradd --uid 10001 --gid darloc --create-home darloc \
    && mkdir -p /app/storage/quittances \
    && chown -R darloc:darloc /app

WORKDIR /app

COPY --chown=darloc:darloc target/service-quittances-0.0.1-SNAPSHOT.jar /app/application.jar

USER darloc

EXPOSE 8084

ENTRYPOINT ["java", "-jar", "/app/application.jar"]