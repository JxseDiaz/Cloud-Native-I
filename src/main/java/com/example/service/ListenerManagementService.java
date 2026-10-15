package com.example.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.listener.MessageListenerContainer;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistry;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ListenerManagementService {

    private final RabbitListenerEndpointRegistry registry;

    /** Pausa el consumo: detiene el container (stop) hasta que se llame resume. */
    public boolean pauseListener(String listenerId) {
        try {
            MessageListenerContainer container = registry.getListenerContainer(listenerId);
            if (container == null) {
                log.warn("Listener no encontrado: {}", listenerId);
                return false;
            }
            if (container.isRunning()) {
                container.stop();
                log.info("⏸ Listener pausado: {}", listenerId);
            }
            return true;
        } catch (Exception e) {
            log.error("Error al pausar listener: {}", e.getMessage());
            return false;
        }
    }

    /** Reanuda el consumo: vuelve a iniciar el container (start). */
    public boolean resumeListener(String listenerId) {
        try {
            MessageListenerContainer container = registry.getListenerContainer(listenerId);
            if (container == null) {
                log.warn("Listener no encontrado: {}", listenerId);
                return false;
            }
            if (!container.isRunning()) {
                container.start();
                log.info("▶ Listener reanudado: {}", listenerId);
            }
            return true;
        } catch (Exception e) {
            log.error("Error al reanudar listener: {}", e.getMessage());
            return false;
        }
    }

    public List<String> getAllListenerIds() {
        return registry.getListenerContainerIds().stream().sorted().toList();
    }
}
