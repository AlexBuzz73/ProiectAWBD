package com.example.gatewayservice;

import com.example.gatewayservice.filters.RateLimiterGatewayFilterFactory;
import com.example.gatewayservice.ratelimit.InMemoryRateLimiter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RateLimiterFilterTest {

    private InMemoryRateLimiter rateLimiter;
    private RateLimiterGatewayFilterFactory filterFactory;

    @BeforeEach
    void setUp() {
        rateLimiter = new InMemoryRateLimiter();
        filterFactory = new RateLimiterGatewayFilterFactory(rateLimiter);
    }

    @Test
    @DisplayName("Requests within capacity succeed")
    void testRequestsWithinCapacityAllowed() {
        var config = new RateLimiterGatewayFilterFactory.Config(3, 0.1);
        GatewayFilter filter = filterFactory.apply(config);

        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());

        MockServerHttpRequest request = MockServerHttpRequest.post("/api/auth/login")
                .remoteAddress(new java.net.InetSocketAddress("127.0.0.1", 1234))
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        // 3 allowed calls
        for (int i = 0; i < 3; i++) {
            StepVerifier.create(filter.filter(exchange, chain))
                    .verifyComplete();
            assertThat(exchange.getResponse().getStatusCode()).isNull();
        }
    }

    @Test
    @DisplayName("Exceeding rate limit produces HTTP 429 Too Many Requests")
    void testExceedingLimitProduces429() {
        var config = new RateLimiterGatewayFilterFactory.Config(2, 0.0);
        GatewayFilter filter = filterFactory.apply(config);

        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());

        MockServerHttpRequest request = MockServerHttpRequest.post("/api/auth/login")
                .remoteAddress(new java.net.InetSocketAddress("10.0.0.1", 1234))
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        // Consume 2 allowed
        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();
        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        // 3rd call should trigger 429
        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(exchange.getResponse().getHeaders().getFirst("Retry-After")).isEqualTo("1");
    }

    @Test
    @DisplayName("Different client IPs have independent rate limit buckets")
    void testDifferentIpsIsolated() {
        var config = new RateLimiterGatewayFilterFactory.Config(1, 0.0);
        GatewayFilter filter = filterFactory.apply(config);

        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());

        MockServerHttpRequest req1 = MockServerHttpRequest.post("/api/auth/login")
                .remoteAddress(new java.net.InetSocketAddress("192.168.1.10", 1234))
                .build();
        MockServerWebExchange ex1 = MockServerWebExchange.from(req1);

        MockServerHttpRequest req2 = MockServerHttpRequest.post("/api/auth/login")
                .remoteAddress(new java.net.InetSocketAddress("192.168.1.20", 1234))
                .build();
        MockServerWebExchange ex2 = MockServerWebExchange.from(req2);

        // req1 consumes its token
        StepVerifier.create(filter.filter(ex1, chain)).verifyComplete();
        assertThat(ex1.getResponse().getStatusCode()).isNull();

        // req2 still has its token
        StepVerifier.create(filter.filter(ex2, chain)).verifyComplete();
        assertThat(ex2.getResponse().getStatusCode()).isNull();
    }
}
