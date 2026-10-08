# syntax=docker/dockerfile:1
FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn --batch-mode --no-transfer-progress clean verify

FROM quay.io/wildfly/wildfly:35.0.1.Final-jdk21

COPY --from=build --chown=jboss:jboss /app/target/wildfly-junit-sample.war /opt/jboss/wildfly/standalone/deployments/wildfly-junit-sample.war

USER jboss
EXPOSE 8080
CMD ["/opt/jboss/wildfly/bin/standalone.sh", "-b", "0.0.0.0"]
