FROM eclipse-temurin:17-jdk-jammy
WORKDIR /app
COPY . .
RUN ./mvnw clean package -DskipTests || mvn clean package -DskipTests
EXPOSE 8080
CMD ["java", "-jar", "target/inventory-complete-1.0.0.jar"]
