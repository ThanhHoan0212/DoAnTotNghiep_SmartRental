# ==========================================
# Stage 1: Build JAR with Maven
# ==========================================
FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /app

# Copy pom.xml first for dependency caching
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build package
COPY src ./src
RUN mvn clean package -DskipTests

# ==========================================
# Stage 2: Minimal Runtime Environment
# ==========================================
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Add non-root user for security best practices
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
