FROM docker.io/library/maven:3.9.10-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests

# Sonda de healthcheck em Java puro: a imagem final (distroless) não tem
# shell, wget nem curl — só o JRE. Compilada aqui, onde há JDK 21.
COPY docker/HealthCheck.java /app/health/HealthCheck.java
RUN javac /app/health/HealthCheck.java

# Crie o diretório de dados e ajuste permissões AQUI (no build)
RUN mkdir -p /app/data && chmod -R 777 /app/data

# Stage 2: Runtime seguro
FROM gcr.io/distroless/java21-debian13:nonroot
LABEL maintainer="Delivery Tech Team"
COPY --from=build /app/target/*.jar /app/delivery-api.jar
COPY --from=build /app/health/HealthCheck.class /app/health/HealthCheck.class

USER nonroot:nonroot
WORKDIR /app
ENV SPRING_PROFILES_ACTIVE=docker
EXPOSE 8080
ENTRYPOINT ["java", "-Xms256m", "-Xmx512m", "-jar", "/app/delivery-api.jar"]