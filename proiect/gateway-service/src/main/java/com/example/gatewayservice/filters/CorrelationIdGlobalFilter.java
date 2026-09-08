package com.example.gatewayservice.filters;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
public class CorrelationIdGlobalFilter implements WebFilter, GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(CorrelationIdGlobalFilter.class);
    public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    public static final String GATEWAY_SERVICE_HEADER = "X-Gateway-Service";
    public static final String GATEWAY_SERVICE_NAME = "gateway-service";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        return processCorrelation(exchange, chain::filter);
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return processCorrelation(exchange, chain::filter);
    }

    private Mono<Void> processCorrelation(ServerWebExchange exchange, java.util.function.Function<ServerWebExchange, Mono<Void>> next) {
        String correlationId = exchange.getRequest().getHeaders().getFirst(CORRELATION_ID_HEADER);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }

        final String finalCorrelationId = correlationId;

        // Propagate X-Correlation-Id downstream
        ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                .header(CORRELATION_ID_HEADER, finalCorrelationId)
                .build();

        // Attach response headers immediately and register beforeCommit
        exchange.getResponse().getHeaders().set(CORRELATION_ID_HEADER, finalCorrelationId);
        exchange.getResponse().getHeaders().set(GATEWAY_SERVICE_HEADER, GATEWAY_SERVICE_NAME);
        exchange.getResponse().beforeCommit(() -> {
            exchange.getResponse().getHeaders().set(CORRELATION_ID_HEADER, finalCorrelationId);
            exchange.getResponse().getHeaders().set(GATEWAY_SERVICE_HEADER, GATEWAY_SERVICE_NAME);
            return Mono.empty();
        });

        // Safe logging without credentials or sensitive data
        log.info("[GATEWAY] [correlationId={}] Incoming request: {} {}",
                finalCorrelationId,
                exchange.getRequest().getMethod(),
                exchange.getRequest().getPath());

        long startTime = System.currentTimeMillis();

        return next.apply(exchange.mutate().request(mutatedRequest).build())
                .doFinally(signalType -> {
                    long duration = System.currentTimeMillis() - startTime;
                    var statusCode = exchange.getResponse().getStatusCode();
                    log.info("[GATEWAY] [correlationId={}] Completed with status={} in {}ms",
                            finalCorrelationId,
                            statusCode,
                            duration);
                });
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
