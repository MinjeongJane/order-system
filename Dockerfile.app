# Dockerfile.app
FROM eclipse-temurin:17-jdk

WORKDIR /app

ADD https://raw.githubusercontent.com/vishnubob/wait-for-it/master/wait-for-it.sh /wait-for-it.sh
RUN chmod +x /wait-for-it.sh

COPY build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["/wait-for-it.sh", "db:3306", "--", "/wait-for-it.sh", "redis:6379", "--", "java", "-jar", "/app/app.jar"]