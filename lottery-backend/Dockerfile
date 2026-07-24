FROM eclipse-temurin:17-jdk-jammy AS build

WORKDIR /workspace

COPY lottery-backend/.mvn .mvn
COPY lottery-backend/mvnw lottery-backend/pom.xml ./

RUN chmod +x mvnw && ./mvnw -B -DskipTests dependency:go-offline

COPY lottery-backend/src ./src

RUN ./mvnw -B -DskipTests package

FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

COPY --from=build /workspace/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
