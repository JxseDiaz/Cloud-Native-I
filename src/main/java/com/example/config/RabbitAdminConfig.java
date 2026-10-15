package com.example.config;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitAdminConfig {

    @Bean
    public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
        return new RabbitAdmin(connectionFactory);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        return new RabbitTemplate(connectionFactory);
    }

    /**
     * Cola usada por OrderListener. RabbitAdmin la declara al conectarse,
     * así el listener no falla si todavía no se creó vía API.
     * (Declarar una cola ya existente con los mismos parámetros es idempotente.)
     */
    @Bean
    public Queue ordersQueue() {
        return new Queue("orders.queue", true);
    }
}
