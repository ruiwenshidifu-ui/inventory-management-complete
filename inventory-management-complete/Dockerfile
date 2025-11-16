# 使用官方 Maven 镜像进行构建
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

# 使用轻量级 JRE 镜像运行
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=build /app/target/inventory-complete-1.0.0.jar app.jar
EXPOSE 8080
CMD ["java", "-jar", "app.jar"]
