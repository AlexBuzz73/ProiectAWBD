package com.example.gatewayservice;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "eureka.client.enabled=false",
        "eureka.client.register-with-eureka=false",
        "eureka.client.fetch-registry=false",
        "logging.level.org.springframework.web.cors=TRACE"
})
@Import({TestJwtConfig.class, TestTokenGenerator.class})
class SecurityAndRoutingTest {

    @Autowired
    private ApplicationContext context;

    @Autowired
    private TestTokenGenerator tokenGenerator;

    @Autowired
    private RouteLocator routeLocator;

    private WebTestClient webClient;

    @BeforeEach
    void setup() {
        this.webClient = WebTestClient.bindToApplicationContext(context)
                .apply(SecurityMockServerConfigurers.springSecurity())
                .configureClient()
                .baseUrl("http://localhost:8090")
                .build();
    }

    @Test
    @DisplayName("Protected endpoint without JWT returns 401 Unauthorized")
    void testProtectedEndpointWithoutJwtReturns401() {
        webClient.get().uri("/api/accounts")
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody()
                .jsonPath("$.status").isEqualTo(401)
                .jsonPath("$.error").isEqualTo("Unauthorized");
    }

    @Test
    @DisplayName("Protected endpoint with invalid JWT returns 401 Unauthorized")
    void testProtectedEndpointWithInvalidJwtReturns401() {
        webClient.get().uri("/api/accounts")
                .header(HttpHeaders.AUTHORIZATION, "Bearer invalid.mock.jwt.signature")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    @DisplayName("USER role accessing ADMIN endpoint returns 403 Forbidden")
    void testUserRoleAccessingAdminEndpointReturns403() {
        String userToken = tokenGenerator.generateToken(1, "user1", "user@test.com", "USER");

        webClient.put().uri("/api/admin/users/5/unlock")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)
                .exchange()
                .expectStatus().isForbidden()
                .expectBody()
                .jsonPath("$.status").isEqualTo(403)
                .jsonPath("$.error").isEqualTo("Forbidden");
    }

    @Test
    @DisplayName("Public auth endpoints are allowed without token")
    void testPublicAuthEndpointsAllowed() {
        // Without downstream services running in unit test, request passes security and attempts routing
        webClient.post().uri("/api/auth/login")
                .exchange()
                .expectStatus().value(status -> assertThat(status).isNotEqualTo(401));
    }

    @Test
    @DisplayName("Public JWKS endpoint is allowed without token")
    void testPublicJwksAllowed() {
        webClient.get().uri("/.well-known/jwks.json")
                .exchange()
                .expectStatus().value(status -> assertThat(status).isNotEqualTo(401));
    }

    @Test
    @DisplayName("Public CSRF endpoint returns 200 OK without token")
    void testCsrfEndpointAllowed() {
        webClient.get().uri("/api/csrf")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.token").isEqualTo("microservices-stateless-jwt");
    }

    @Test
    @DisplayName("CORS preflight request succeeds with appropriate CORS headers")
    void testCorsPreflight() {
        webClient.options().uri("/api/accounts")
                .header("Origin", "http://localhost:5173")
                .header("Access-Control-Request-Method", "GET")
                .header("Access-Control-Request-Headers", "Authorization")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals("Access-Control-Allow-Origin", "http://localhost:5173")
                .expectHeader().valueEquals("Access-Control-Allow-Credentials", "true");
    }

    @Test
    @DisplayName("RouteLocator registers expected microservice routes")
    void testRouteLocatorRegistersExpectedRoutes() {
        var routes = routeLocator.getRoutes().collectList().block();
        assertThat(routes).isNotNull().isNotEmpty();

        var routeIds = routes.stream().map(r -> r.getId()).toList();
        assertThat(routeIds).contains(
                "auth-login",
                "auth-register",
                "user-categories",
                "user-cards",
                "user-limits",
                "admin-users",
                "admin-accounts",
                "user-service",
                "account-service",
                "transaction-payments",
                "transaction-service"
        );

        // Verify URIs use lb://
        for (var route : routes) {
            String uriStr = route.getUri().toString();
            assertThat(uriStr).startsWith("lb://");
        }
    }
}
