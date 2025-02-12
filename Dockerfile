# Use official Ubuntu 24.04 (Noble) as the base image
FROM --platform=linux/arm64 ubuntu:24.04

# Set non-interactive mode for apt
ENV DEBIAN_FRONTEND=noninteractive

# Add a retry mechanism for apt update
RUN apt-get clean && apt-get update -o Acquire::Retries=3 && apt-get install -y \
    openjdk-17-jdk \
    curl \
    && rm -rf /var/lib/apt/lists/*

# Set JAVA_HOME environment variable
ENV JAVA_HOME=/usr/lib/jvm/java-17-openjdk-arm64
ENV PATH="${JAVA_HOME}/bin:${PATH}"

WORKDIR /app
COPY userdetails-cognito/build/libs/*-exec.war app.war
CMD ["java", "-jar", "app.war"]