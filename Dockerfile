# Use a base image with Java
FROM openjdk:17-jdk-alpine

RUN apk update && apk add ffmpeg

# Set the working directory
WORKDIR /app

# Copy the WAR file to the container
COPY build/libs/spring.war app/app.war

# Expose the port your application will run on
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "app/app.war"]
