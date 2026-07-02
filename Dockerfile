# Stage 1: Build stage
FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /app

# Copy gradle files
COPY gradle/ /app/gradle/
COPY gradlew /app/
COPY build.gradle settings.gradle /app/

# Grant execute permission for gradlew and download dependencies
RUN chmod +x gradlew
RUN ./gradlew dependencies --no-daemon

# Copy source code and build jar
COPY src /app/src
RUN ./gradlew bootJar -x test --no-daemon

# Stage 2: Run stage
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Copy the built jar from build stage
COPY --from=build /app/build/libs/*.jar app.jar

# Expose port
EXPOSE 8080

# Run the app
ENTRYPOINT ["java", "-jar", "app.jar"]
