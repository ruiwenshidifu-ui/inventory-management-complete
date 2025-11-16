# 使用阿里云镜像源，解决网络问题
FROM maven:3.9.6-eclipse-temurin-17-alpine AS builder

# 设置阿里云镜像源
RUN echo "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" > /tmp/settings.xml && \
    echo "<settings xmlns=\"http://maven.apache.org/SETTINGS/1.0.0\"" >> /tmp/settings.xml && \
    echo "          xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" >> /tmp/settings.xml && \
    echo "          xsi:schemaLocation=\"http://maven.apache.org/SETTINGS/1.0.0 https://maven.apache.org/xsd/settings-1.0.0.xsd\">" >> /tmp/settings.xml && \
    echo "  <mirrors>" >> /tmp/settings.xml && \
    echo "    <mirror>" >> /tmp/settings.xml && \
    echo "      <id>alimaven</id>" >> /tmp/settings.xml && \
    echo "      <name>aliyun maven</name>" >> /tmp/settings.xml && \
    echo "      <url>https://maven.aliyun.com/repository/public</url>" >> /tmp/settings.xml && \
    echo "      <mirrorOf>central</mirrorOf>" >> /tmp/settings.xml && \
    echo "    </mirror>" >> /tmp/settings.xml && \
    echo "  </mirrors>" >> /tmp/settings.xml && \
    echo "</settings>" >> /tmp/settings.xml

WORKDIR /build

# 复制所有文件，一次性解决
COPY . .

# 强制清理并重新打包，解决所有缓存问题
RUN mvn clean compile package -DskipTests -s /tmp/settings.xml

# 运行阶段
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# 直接从构建阶段复制，避免路径问题
COPY --from=builder /build/target/inventory-complete-1.0.0.jar app.jar

EXPOSE 8080

# 直接运行，不搞复杂权限
CMD ["java", "-jar", "app.jar"]
