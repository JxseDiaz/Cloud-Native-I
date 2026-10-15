package com.example.controller;

import com.example.service.ListenerManagementService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/listeners")
@RequiredArgsConstructor
public class ListenerController {

    private final ListenerManagementService listenerService;

    @GetMapping
    public ResponseEntity<Object> getAllListeners() {
        return ResponseEntity.ok(listenerService.getAllListenerIds());
    }

    @PostMapping("/{listenerId}/pause")
    public ResponseEntity<String> pauseListener(@PathVariable String listenerId) {
        return listenerService.pauseListener(listenerId)
                ? ResponseEntity.ok("Listener pausado: " + listenerId)
                : ResponseEntity.status(404).body("Listener no encontrado: " + listenerId);
    }

    @PostMapping("/{listenerId}/resume")
    public ResponseEntity<String> resumeListener(@PathVariable String listenerId) {
        return listenerService.resumeListener(listenerId)
                ? ResponseEntity.ok("Listener reanudado: " + listenerId)
                : ResponseEntity.status(404).body("Listener no encontrado: " + listenerId);
    }
}
