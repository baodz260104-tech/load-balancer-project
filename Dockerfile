# Giai đoạn 1: Dùng Maven kèm JDK để biên dịch code Java
FROM maven:3.9-eclipse-temurin-21-alpine AS builder

WORKDIR /app

# Copy pom.xml và tải dependencies
COPY pom.xml .
RUN mvn dependency:go-offline

# Copy mã nguồn và đóng gói thành file .jar
COPY src ./src
RUN mvn clean package -DskipTests

# Giai đoạn 2: Dùng môi trường chạy JRE siêu nhẹ (chỉ khoảng vài chục MB)
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Lấy file jar đã build từ Giai đoạn 1
COPY --from=builder /app/target/*.jar app.jar

# Mở cổng 8000 của Load Balancer ra bên ngoài
EXPOSE 8000

# Lệnh khởi chạy ứng dụng khi container bật
ENTRYPOINT ["java", "-jar", "app.jar"]