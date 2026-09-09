package com.example.gatewayservice.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
public class CsrfController {

    @GetMapping({"/api/csrf", "/csrf"})
    public Mono<ResponseEntity<Map<String, String>>> getCsrf() {
        return Mono.just(ResponseEntity.ok(Map.of(
                "headerName", "X-XSRF-TOKEN",
                "parameterName", "_csrf",
                "token", "microservices-stateless-jwt"
        )));
    }
}
