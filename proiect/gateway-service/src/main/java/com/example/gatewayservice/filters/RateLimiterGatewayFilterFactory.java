package com.example.gatewayservice.filters;

import com.example.gatewayservice.ratelimit.InMemoryRateLimiter;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

@Component
public class RateLimiterGatewayFilterFactory extends AbstractGatewayFilterFactory<RateLimiterGatewayFilterFactory.Config> {

    private final InMemoryRateLimiter rateLimiter;

    public RateLimiterGatewayFilterFactory(InMemoryRateLimiter rateLimiter) {
        super(Config.class);
        this.rateLimiter = rateLimiter;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String clientIp = "unknown";
            var remoteAddress = exchange.getRequest().getRemoteAddress();
            if (remoteAddress != null && remoteAddress.getAddress() != null) {
                clientIp = remoteAddress.getAddress().getHostAddress();
            }
            String forwardedFor = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
            if (forwardedFor != null && !forwardedFor.isBlank()) {
                clientIp = forwardedFor.split(",")[0].trim();
            }

            String path = exchange.getRequest().getPath().value();
            String key = clientIp + ":" + path;

            boolean allowed = rateLimiter.tryAcquire(key, config.getCapacity(), config.getRefillRate());
            if (!allowed) {
                exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
                exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
                exchange.getResponse().getHeaders().set("Retry-After", "1");
                byte[] bytes = "{\"error\":\"Too Many Requests\",\"message\":\"Rata de solicitari permisa a fost depasita. Incercati din nou mai tarziu.\",\"status\":429}".getBytes(StandardCharsets.UTF_8);
                DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
                return exchange.getResponse().writeWith(Mono.just(buffer));
            }

            return chain.filter(exchange);
        };
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Config {
        private int capacity = 10;
        private double refillRate = 2.0;
    }
}
