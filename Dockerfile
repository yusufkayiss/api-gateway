# 1. Aşama: Build (Maven + JDK 21)
FROM maven:3.9.9-eclipse-temurin-21-alpine AS build
WORKDIR /app

# Bağımlılıkları önbelleğe almak için önce pom.xml kopyalanır
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Kaynak kodları kopyalayıp JAR paketini oluştur
COPY src ./src
RUN mvn clean package -DskipTests -B

# 2. Aşama: Runtime (Hafif JRE 21 + Non-Root Güvenlik)
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]