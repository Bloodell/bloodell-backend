FROM eclipse-temurin:25-jdk AS build
WORKDIR /src
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline
COPY src/ src/
RUN ./mvnw -B -DskipTests package

FROM eclipse-temurin:25-jre
RUN apt-get update && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && useradd --system --create-home --uid 10001 bloodell
WORKDIR /app
RUN mkdir -p /app/logs && chown bloodell:bloodell /app/logs
COPY --from=build /src/target/*.jar /app/bloodell.jar
USER bloodell
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/bloodell.jar"]
