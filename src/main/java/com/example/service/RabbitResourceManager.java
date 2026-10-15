package com.example.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class RabbitResourceManager {

    private final RabbitAdmin rabbitAdmin;

    public void createQueue(String queueName) {
        try {
            Queue queue = QueueBuilder.durable(queueName).build();
            rabbitAdmin.declareQueue(queue);
            log.info("✓ Cola creada: {}", queueName);
        } catch (Exception e) {
            log.error("✗ Error al crear cola {}: {}", queueName, e.getMessage());
        }
    }

    public void createExchange(String exchangeName) {
        try {
            Exchange exchange = new DirectExchange(exchangeName, true, false);
            rabbitAdmin.declareExchange(exchange);
            log.info("✓ Exchange creado: {}", exchangeName);
        } catch (Exception e) {
            log.error("✗ Error al crear exchange {}: {}", exchangeName, e.getMessage());
        }
    }

    public void createBinding(String queueName, String exchangeName, String routingKey) {
        try {
            Binding binding = BindingBuilder
                    .bind(new Queue(queueName))
                    .to(new DirectExchange(exchangeName))
                    .with(routingKey);
            rabbitAdmin.declareBinding(binding);
            log.info("✓ Binding creado: {} -> {}", queueName, exchangeName);
        } catch (Exception e) {
            log.error("✗ Error al crear binding: {}", e.getMessage());
        }
    }

    public QueueInformation getQueueInfo(String queueName) {
        try {
            QueueInformation info = rabbitAdmin.getQueueInfo(queueName);
            if (info != null) {
                log.info("Cola: {}, Mensajes: {}, Consumidores: {}",
                        info.getName(), info.getMessageCount(), info.getConsumerCount());
                return info;
            }
            return null;
        } catch (Exception e) {
            log.error("Error al obtener info de cola {}: {}", queueName, e.getMessage());
            return null;
        }
    }

    public void deleteQueue(String queueName) {
        try {
            rabbitAdmin.deleteQueue(queueName);
            log.info("✓ Cola eliminada: {}", queueName);
        } catch (Exception e) {
            log.error("✗ Error al eliminar cola {}: {}", queueName, e.getMessage());
        }
    }

    public void deleteExchange(String exchangeName) {
        try {
            rabbitAdmin.deleteExchange(exchangeName);
            log.info("✓ Exchange eliminado: {}", exchangeName);
        } catch (Exception e) {
            log.error("✗ Error al eliminar exchange {}: {}", exchangeName, e.getMessage());
        }
    }

    public void purgeQueue(String queueName) {
        try {
            int purged = rabbitAdmin.purgeQueue(queueName);
            log.info("✓ Cola purgada: {}, mensajes eliminados: {}", queueName, purged);
        } catch (Exception e) {
            log.error("✗ Error al purgar cola {}: {}", queueName, e.getMessage());
        }
    }
}
