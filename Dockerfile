FROM --platform=linux/amd64 adoptopenjdk/openjdk11:alpine
WORKDIR /app
COPY userdetails-cognito/build/libs/userdetails-cognito-4.0.0-SNAPSHOT-exec.war app.war
CMD ["java", "-jar", "app.war"]