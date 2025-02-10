FROM --platform=linux/amd64 eclipse-temurin:17-jdk-alpine
WORKDIR /app
COPY userdetails-cognito/build/libs/*-exec.war app.war
CMD ["java", "-jar", "app.war"]