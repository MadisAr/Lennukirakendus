# Use a base image with OpenJDK 17
FROM openjdk:23-jdk-slim

# Set the working directory in the container
WORKDIR /app

# Copy the JAR file to the container
COPY target/lennuRakendus-0.0.1-SNAPSHOT.jar /app/lennuRakendus-0.0.1-SNAPSHOT.jar

# Expose the port your app will run on
EXPOSE 8080

# Run the Spring Boot application
ENTRYPOINT ["java", "-jar", "/app/lennuRakendus-0.0.1-SNAPSHOT.jar"]