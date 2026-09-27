FROM maven:3.9.11-eclipse-temurin-21-alpine AS build

WORKDIR /build
COPY pom.xml ./
RUN mvn --batch-mode --no-transfer-progress dependency:go-offline
COPY src ./src
RUN mvn --batch-mode --no-transfer-progress -DskipTests package

FROM eclipse-temurin:21-jre-alpine

RUN addgroup -S studyflow && adduser -S studyflow -G studyflow
WORKDIR /app
COPY --from=build /build/target/studyflow-1.0.0.jar ./studyflow.jar
COPY web ./web
RUN mkdir -p /var/data && chown -R studyflow:studyflow /app /var/data

USER studyflow
ENV STUDYFLOW_HOST=0.0.0.0 \
    STUDYFLOW_WEB_DIR=/app/web \
    STUDYFLOW_DATA_DIR=/var/data
EXPOSE 10000

CMD ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/studyflow.jar"]
