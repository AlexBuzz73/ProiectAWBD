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
                                new RateLimiterGatewayFilterFactory.Config(30, 5.0))))
                        .uri("lb://user-service"))
                .route("auth-register", r -> r
                        .path("/api/auth/register", "/api/auth/validate-individual")
                        .filters(f -> f.filter(rateLimiterFilterFactory.apply(
                                new RateLimiterGatewayFilterFactory.Config(30, 5.0))))
                        .uri("lb://user-service"))
                .route("auth-public", r -> r
                        .path("/api/auth/**", "/api/auth")
                        .uri("lb://user-service"))
                .route("jwks-public", r -> r
                        .path("/.well-known/jwks.json")
                        .uri("lb://user-service"))

                // 2. Specific routes under /api/users/** that belong to Transaction Service (Categories)
                .route("user-categories", r -> r
                        .path("/api/users/me/categories/**", "/api/users/me/categories",
                              "/api/users/*/categories/**", "/api/users/*/categories")
                        .uri("lb://transaction-service"))

                // 3. Specific routes under /api/users/** that belong to Account Service (Cards)
                .route("user-cards", r -> r
                        .path("/api/users/me/accounts/*/card/**", "/api/users/me/accounts/*/card",
                              "/api/users/*/accounts/*/card/**", "/api/users/*/accounts/*/card")
                        .uri("lb://account-service"))

                // 4. User limits routes that belong to Account Service (/api/user/me/limits and /api/limits)
                .route("user-limits", r -> r
                        .path("/api/user/me/limits/**", "/api/user/me/limits",
                              "/api/user/*/limits/**", "/api/user/*/limits",
                              "/api/limits/**", "/api/limits")
                        .uri("lb://account-service"))

                // 5. Admin Routes - Explicit per downstream service (no collision)
                .route("admin-users", r -> r
                        .path("/api/admin/users/**", "/api/admin/users", "/api/admin/unlock-user")
                        .uri("lb://user-service"))
                .route("admin-accounts", r -> r
                        .path("/api/admin/accounts/**", "/api/admin/accounts",
                              "/api/admin/bank-limits/**", "/api/admin/bank-limits",
                              "/api/admin/create-shared-account")
                        .uri("lb://account-service"))

                // 6. User Service Domain Routes (profile, user lookup)
                .route("user-service", r -> r
                        .path("/api/users/**", "/api/users")
                        .uri("lb://user-service"))

                // 7. Account Service Domain Routes (accounts, card operations)
                .route("account-service", r -> r
                        .path("/api/accounts/**", "/api/accounts")
                        .uri("lb://account-service"))

                // 8. Transaction Service Domain Routes (payments with rate limiting, transactions, categories, tags)
                .route("transaction-payments", r -> r
                        .path("/api/payments/**", "/api/payments")
                        .filters(f -> f.filter(rateLimiterFilterFactory.apply(
                                new RateLimiterGatewayFilterFactory.Config(30, 10.0))))
                        .uri("lb://transaction-service"))
                .route("transaction-service", r -> r
                        .path("/api/transactions/**", "/api/transactions",
                              "/api/categories/**", "/api/categories",
                              "/api/tags/**", "/api/tags")
                        .uri("lb://transaction-service"))
                .build();
    }
}
