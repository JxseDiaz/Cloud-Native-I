# rabbitmq-avanzado

Guía 2.3.1 – RabbitMQ Cluster con Docker Compose + Spring Boot (RabbitAdmin y control de listeners).

## 1. Levantar el cluster
    docker compose up -d
    docker compose ps
    docker exec rabbitmq-1 rabbitmqctl cluster_status

Management UI: http://localhost:15672 (nodo 1) y http://localhost:15673 (nodo 2) — admin / admin123

## 2. Ejecutar Spring Boot (JDK 17 o 21)
    mvn clean install
    mvn spring-boot:run

## 3. Probar endpoints
    curl -X POST "http://localhost:8080/api/rabbitmq/queues?queueName=orders.queue"
    curl http://localhost:8080/api/listeners
    curl -X POST "http://localhost:8080/api/listeners/order-listener/pause"
    curl -X POST "http://localhost:8080/api/listeners/order-listener/resume"

(En PowerShell usa `curl.exe` en vez de `curl`.)
