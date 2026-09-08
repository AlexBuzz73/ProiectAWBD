package com.example.gatewayservice.config;

import com.example.gatewayservice.filters.RateLimiterGatewayFilterFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class GatewayRoutesConfig {

    private final RateLimiterGatewayFilterFactory rateLimiterFilterFactory;

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                // 1. User Service - Public Auth with Rate Limiting on Login & Register
                .route("auth-login", r -> r
                        .path("/api/auth/login")
                        .filters(f -> f.filter(rateLimiterFilterFactory.apply(
                                new RateLimiterGatewayFilterFactory.Config(10, 2.0))))
                        .uri("lb://user-service"))
                .route("auth-register", r -> r
                        .path("/api/auth/register", "/api/auth/validate-individual")
                        .filters(f -> f.filter(rateLimiterFilterFactory.apply(
                                new RateLimiterGatewayFilterFactory.Config(10, 5.0))))
                        .uri("lb://user-service"))
                .route("auth-public", r -> r
                        .path("/api/auth/**")
                        .uri("lb://user-service"))
                .route("jwks-public", r -> r
                        .path("/.well-known/jwks.json")
                        .uri("lb://user-service"))

                // 2. Admin Routes - Explicit per downstream service (no collision)
                .route("admin-users", r -> r
                        .path("/api/admin/users/**", "/api/admin/unlock-user")
                        .uri("lb://user-service"))
                .route("admin-accounts", r -> r
                        .path("/api/admin/accounts/**", "/api/admin/bank-limits/**", "/api/admin/create-shared-account")
                        .uri("lb://account-service"))

                // 3. User Service Domain Routes
                .route("user-service", r -> r
                        .path("/api/users/**")
                        .uri("lb://user-service"))

                // 4. Account Service Domain Routes
                .route("account-service", r -> r
                        .path("/api/accounts/**", "/api/limits/**")
                        .uri("lb://account-service"))

                // 5. Transaction Service Domain Routes
                .route("transaction-payments", r -> r
                        .path("/api/payments/**")
                        .filters(f -> f.filter(rateLimiterFilterFactory.apply(
                                new RateLimiterGatewayFilterFactory.Config(15, 5.0))))
                        .uri("lb://transaction-service"))
                .route("transaction-service", r -> r
                        .path("/api/transactions/**", "/api/categories/**", "/api/tags/**")
                        .uri("lb://transaction-service"))
                .build();
    }
}
