# 使用包含 Java 和 Maven 的基础镜像
FROM maven:3.8.6-openjdk-17 AS build

# 设置工作目录
WORKDIR /app

# 复制 pom.xml 和 src 目录
COPY pom.xml .
COPY src ./src

# 构建项目
RUN mvn clean package -DskipTests

# 第二阶段：使用更小的 JRE 镜像运行应用
FROM openjdk:17-jre-slim

# 设置工作目录
WORKDIR /app

# 从构建阶段复制打包好的 JAR 文件
COPY --from=build /app/target/*.jar app.jar

# 暴露应用端口（默认 Spring Boot 是 8080）
EXPOSE 8080

# 运行应用
ENTRYPOINT ["java", "-jar", "app.jar"]
