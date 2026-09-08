package com.example.gatewayservice;

import com.example.gatewayservice.filters.CorrelationIdGlobalFilter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class CorrelationIdFilterTest {

    private final CorrelationIdGlobalFilter filter = new CorrelationIdGlobalFilter();

    @Test
    @DisplayName("Generates new UUID when X-Correlation-Id is missing")
    void testGeneratesMissingCorrelationId() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/users/me").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        AtomicReference<String> downstreamHeader = new AtomicReference<>();
        GatewayFilterChain chain = ex -> {
            downstreamHeader.set(ex.getRequest().getHeaders().getFirst("X-Correlation-Id"));
            return Mono.empty();
        };

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        assertThat(downstreamHeader.get()).isNotNull().isNotBlank();
        // Verify response header is also set
        assertThat(exchange.getResponse().getHeaders().getFirst("X-Gateway-Service")).isEqualTo("gateway-service");
        assertThat(exchange.getResponse().getHeaders().getFirst("X-Correlation-Id")).isEqualTo(downstreamHeader.get());
    }

    @Test
    @DisplayName("Propagates existing X-Correlation-Id when provided by client")
    void testPropagatesExistingCorrelationId() {
        String existingId = "client-trace-12345";
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/accounts")
                .header("X-Correlation-Id", existingId)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        AtomicReference<String> downstreamHeader = new AtomicReference<>();
        GatewayFilterChain chain = ex -> {
            downstreamHeader.set(ex.getRequest().getHeaders().getFirst("X-Correlation-Id"));
            return Mono.empty();
        };

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        assertThat(downstreamHeader.get()).isEqualTo(existingId);
        assertThat(exchange.getResponse().getHeaders().getFirst("X-Correlation-Id")).isEqualTo(existingId);
    }
}
