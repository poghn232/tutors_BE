FROM eclipse-temurin:17-jdk

WORKDIR /app

COPY . .

RUN chmod +x mvnw || true
RUN ./mvnw clean package -DskipTests || mvn clean package -DskipTests

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "target/tutors-be-0.0.1-SNAPSHOT.jar"]
