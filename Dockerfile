FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

COPY src src
RUN ./mvnw clean package -DskipTests -B

FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=build /workspace/target/*.jar app.jar

ENV JAVA_TOOL_OPTIONS="-Duser.timezone=UTC"

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]