# 构建阶段
FROM maven:3.9.6-eclipse-temurin-17-alpine AS builder
WORKDIR /build
# 先只复制POM文件，利用Docker缓存层
COPY pom.xml .
# 验证Maven配置并下载依赖（利用缓存，除非POM改变）
RUN mvn dependency:go-offline
# 复制所有源代码
COPY src ./src
# 编译和打包应用
RUN mvn clean package -DskipTests

# 运行阶段
FROM eclipse-temurin:17-jre-alpine
# 创建一个非root用户以增强安全
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring
WORKDIR /app
# 从构建阶段复制打包好的应用
COPY --from=builder /build/target/inventory-complete-1.0.0.jar app.jar
# 声明运行时监听的端口
EXPOSE 8080
# 使用 exec 形式启动应用，以便正确处理信号
ENTRYPOINT ["java", "-jar", "app.jar"]
