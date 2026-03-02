FROM eclipse-temurin:17-jdk AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN apt-get update && apt-get install -y maven && mvn clean package -DskipTests

FROM eclipse-temurin:17-jre
COPY --from=build /app/target/*.jar /app.jar
EXPOSE 8802
ENTRYPOINT ["java", "-jar", "/app.jar"]
