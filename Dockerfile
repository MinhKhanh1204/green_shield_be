FROM eclipse-temurin:17-jdk AS build
WORKDIR /app

# Tận dụng cache dependency của Gradle
COPY gradlew gradlew.bat build.gradle settings.gradle /app/
COPY gradle /app/gradle
RUN chmod +x /app/gradlew

# Copy source sau để cache tốt hơn
COPY src /app/src

# Build Spring Boot jar
RUN ./gradlew --no-daemon clean bootJar

FROM eclipse-temurin:17-jre
WORKDIR /app

# Copy jar build ra image runtime
COPY --from=build /app/build/libs/*.jar /app/app.jar

EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
