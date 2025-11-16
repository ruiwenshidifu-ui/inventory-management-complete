# 使用官方 Maven 镜像进行构建阶段
FROM maven:3.8.6-openjdk-17-slim AS build
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

# 使用更小的 JRE 镜像运行应用
FROM openjdk:17-slim
WORKDIR /app
COPY --from=build /app/target/inventory-complete-1.0.0.jar app.jar
EXPOSE 8080
CMD ["java", "-jar", "app.jar"]
