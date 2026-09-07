package com.example.userservice.controllers;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/internal/users")
public class InternalUserController {

    @Value("${server.port:8081}")
    private int port;

    @GetMapping("/instance-info")
    public ResponseEntity<Map<String, Object>> getInstanceInfo() {
        return ResponseEntity.ok(Map.of("service", "user-service", "port", port));
    }
}
