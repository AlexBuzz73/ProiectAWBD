package com.example.gatewayservice.exceptions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebExceptionHandler;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

@Component
@Order(-2)
public class GatewayExceptionHandler implements WebExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GatewayExceptionHandler.class);

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        if (exchange.getResponse().isCommitted()) {
            return Mono.error(ex);
        }

        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        String message = "A aparut o eroare in procesarea cererii prin Gateway.";

        if (ex instanceof ResponseStatusException rse) {
            status = HttpStatus.resolve(rse.getStatusCode().value()) != null
                    ? HttpStatus.valueOf(rse.getStatusCode().value())
                    : HttpStatus.INTERNAL_SERVER_ERROR;
            message = rse.getReason() != null ? rse.getReason() : status.getReasonPhrase();
        } else if (ex.getClass().getSimpleName().contains("NotFound")
                || (ex.getMessage() != null && ex.getMessage().contains("Unable to find instance"))) {
            status = HttpStatus.SERVICE_UNAVAILABLE;
            message = "Serviciul solicitat nu este disponibil in registry.";
        }

        log.error("[GATEWAY] Error handling request {}: {}", exchange.getRequest().getPath(), ex.getMessage());

        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String escapedMessage = message != null ? message.replace("\"", "'") : "";
        String json = String.format("{\"error\":\"%s\",\"status\":%d,\"message\":\"%s\"}",
                status.getReasonPhrase(), status.value(), escapedMessage);
        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(json.getBytes(StandardCharsets.UTF_8));
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }
}
