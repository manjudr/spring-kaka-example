FROM openjdk:17-slim
RUN apt-get update && apt-get install -y netcat-traditional && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY build/libs/catalog-publish-1.0.0-SNAPSHOT.jar app.jar
COPY wait-for-kafka.sh .
RUN chmod +x wait-for-kafka.sh
ENTRYPOINT ["./wait-for-kafka.sh", "kafka:9092", "java", "-jar", "app.jar"]
