FROM openjdk:17-slim
RUN apt-get update && apt-get install -y netcat-traditional && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY build/libs/*.jar app.jar
COPY wait-for-kafka.sh .
RUN chmod +x wait-for-kafka.sh
ENTRYPOINT ["./wait-for-kafka.sh", "kafka:9092", "java", "-jar", "app.jar"]
