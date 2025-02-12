FROM eclipse-temurin:17-jdk

WORKDIR /app
COPY userdetails-cognito/build/libs/*-exec.war app.war
CMD ["java", "-jar", "app.war"]