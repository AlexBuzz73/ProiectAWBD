package com.example.transactionservice.exceptions;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        log.warn("Validation error on transaction-service: {}", ex.getMessage());
        List<String> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("error", "Campurile furnizate sunt invalide");
        response.put("details", errors);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("Illegal argument on transaction-service: {}", ex.getMessage());
        Map<String, String> body = new HashMap<>();
        body.put("error", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleIllegalState(IllegalStateException ex) {
        log.warn("Illegal state on transaction-service: {}", ex.getMessage());
        Map<String, String> body = new HashMap<>();
        body.put("error", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class
    })
    public ResponseEntity<Map<String, String>> handleBadRequestExceptions(Exception ex) {
        log.warn("Bad request on transaction-service: {}", ex.getMessage());
        Map<String, String> body = new HashMap<>();
        body.put("error", "Datele transmise sunt invalide sau lipsesc");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler({
            ResourceNotFoundException.class,
            NoResourceFoundException.class,
            NoHandlerFoundException.class
    })
    public ResponseEntity<Map<String, String>> handleNotFoundExceptions(Exception ex) {
        log.warn("Resource not found on transaction-service: {}", ex.getMessage());
        Map<String, String> body = new HashMap<>();
        body.put("error", ex.getMessage() != null && !ex.getMessage().isBlank() ? ex.getMessage() : "Resursa nu a fost gasita");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    @ExceptionHandler({
            ServiceUnavailableException.class,
            io.github.resilience4j.circuitbreaker.CallNotPermittedException.class
    })
    public ResponseEntity<Map<String, Object>> handleServiceUnavailable(Exception ex, jakarta.servlet.http.HttpServletRequest request) {
        log.error("Downstream service unavailable on transaction-service: {}", ex.getMessage());
        String correlationId = request != null ? request.getHeader("X-Correlation-Id") : null;
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = java.util.UUID.randomUUID().toString();
        }
        Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("error", "Service Unavailable");
        body.put("message", ex instanceof ServiceUnavailableException && ex.getMessage() != null && !ex.getMessage().isBlank()
                ? ex.getMessage()
                : "Serviciul de conturi nu este disponibil momentan.");
        body.put("status", HttpStatus.SERVICE_UNAVAILABLE.value());
        body.put("correlationId", correlationId);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(body);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Map<String, String>> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        log.warn("HTTP method not supported on transaction-service: {}", ex.getMessage());
        Map<String, String> body = new HashMap<>();
        body.put("error", "Metoda HTTP nu este suportata");
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(body);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, String>> handleAccessDenied(AccessDeniedException ex) {
        log.warn("Access denied on transaction-service: {}", ex.getMessage());
        Map<String, String> body = new HashMap<>();
        body.put("error", ex.getMessage() != null && !ex.getMessage().isBlank() ? ex.getMessage() : "Acces interzis");
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, String>> handleAuthenticationException(AuthenticationException ex) {
        log.warn("Authentication error on transaction-service: {}", ex.getMessage());
        Map<String, String> body = new HashMap<>();
        body.put("error", "Autentificare necesara");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGenericException(Exception ex) {
        log.error("Unhandled exception in transaction-service", ex);
        Map<String, String> body = new HashMap<>();
        body.put("error", "A aparut o eroare neasteptata. Va rugam incercati din nou.");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}
