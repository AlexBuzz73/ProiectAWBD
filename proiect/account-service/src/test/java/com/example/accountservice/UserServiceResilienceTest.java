package com.example.accountservice;

import com.example.accountservice.client.CustomFeignErrorDecoder;
import com.example.accountservice.client.FeignAuthInterceptor;
import com.example.accountservice.client.UserClient;
import com.example.accountservice.client.UserFeignClient;
import com.example.accountservice.dto.UserLookupDTO;
import com.example.accountservice.exceptions.ResourceNotFoundException;
import com.example.accountservice.exceptions.ServiceUnavailableException;
import feign.Request;
import feign.Response;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({TestJwtConfig.class, TestTokenGenerator.class})
class UserServiceResilienceTest {

    @Autowired
    private UserClient userClient;

    @MockitoBean
    private UserFeignClient userFeignClient;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @Autowired
    private RetryRegistry retryRegistry;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TestTokenGenerator tokenGenerator;

    private CircuitBreaker circuitBreaker;

    @BeforeEach
    void setUp() {
        circuitBreaker = circuitBreakerRegistry.circuitBreaker("userServiceCircuitBreaker");
        circuitBreaker.reset();
        reset(userFeignClient);
    }

    @Test
    @DisplayName("1. Healthy user-service -> lookup success and circuit stays CLOSED")
    void testHealthyLookup() {
        UserLookupDTO mockUser = new UserLookupDTO(10, "alex", "alex@test.com", "USER", true);
        when(userFeignClient.getUserById(10)).thenReturn(mockUser);

        Optional<UserLookupDTO> result = userClient.findUserById(10);
        assertThat(result).isPresent();
        assertThat(result.get().getUsername()).isEqualTo("alex");
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
    }

    @Test
    @DisplayName("2. Transient failure on GET -> Retry succeeds on 2nd attempt")
    void testTransientFailure_RetrySuccess() {
        Request request = Request.create(Request.HttpMethod.GET, "/api/users/11", Collections.emptyMap(), null, StandardCharsets.UTF_8, null);
        Response errorResponse = Response.builder().status(503).reason("Service Unavailable").request(request).headers(Collections.emptyMap()).build();
        UserLookupDTO mockUser = new UserLookupDTO(11, "radu", "radu@test.com", "USER", true);

        when(userFeignClient.getUserById(11))
                .thenThrow(new ServiceUnavailableException("User service down"))
                .thenReturn(mockUser);

        Optional<UserLookupDTO> result = userClient.findUserById(11);
        assertThat(result).isPresent();
        assertThat(result.get().getUsername()).isEqualTo("radu");
        verify(userFeignClient, times(2)).getUserById(11);
    }

    @Test
    @DisplayName("3 & 4. Repeated failures -> Circuit Breaker OPEN -> Fail fast on subsequent calls")
    void testRepeatedFailures_CircuitBreakerOpen_FailFast() {
        when(userFeignClient.getUserById(anyInt()))
                .thenThrow(new ServiceUnavailableException("User service offline"));

        // Generate 5 failures to cross failureRateThreshold (50% of min 5 calls)
        for (int i = 0; i < 5; i++) {
            final int id = 100 + i;
            assertThatThrownBy(() -> userClient.findUserById(id))
                    .isInstanceOf(ServiceUnavailableException.class);
        }

        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);

        // Next call must fail fast without calling remote user-service
        reset(userFeignClient);
        assertThatThrownBy(() -> userClient.findUserById(999))
                .isInstanceOf(ServiceUnavailableException.class)
                .hasMessageContaining("Serviciul de utilizatori nu este disponibil");

        verifyNoInteractions(userFeignClient);
    }

    @Test
    @DisplayName("5 & 6. State transitions: OPEN -> HALF_OPEN -> success -> CLOSED")
    void testCircuitBreakerTransitionToHalfOpenAndClosed() {
        circuitBreaker.transitionToOpenState();
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);

        circuitBreaker.transitionToHalfOpenState();
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.HALF_OPEN);

        UserLookupDTO user1 = new UserLookupDTO(201, "u1", "u1@test.com", "USER", true);
        UserLookupDTO user2 = new UserLookupDTO(202, "u2", "u2@test.com", "USER", true);
        when(userFeignClient.getUserById(201)).thenReturn(user1);
        when(userFeignClient.getUserById(202)).thenReturn(user2);

        // Execute permitted calls in half-open state (permittedNumberOfCallsInHalfOpenState = 2)
        userClient.findUserById(201);
        userClient.findUserById(202);

        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
    }

    @Test
    @DisplayName("7. Fallback returns controlled HTTP 503 with Correlation ID and error format")
    void testControlled503Fallback() throws Exception {
        when(userFeignClient.getUserById(50))
                .thenThrow(new ServiceUnavailableException("user-service cluster unreachable"));

        mockMvc.perform(get("/api/internal/accounts/feign-test/user/50")
                        .header("X-Correlation-Id", "corr-test-resilience-123"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error").value("Service Unavailable"))
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.message").value("Serviciul de utilizatori nu este disponibil momentan."))
                .andExpect(jsonPath("$.correlationId").value("corr-test-resilience-123"));
    }

    @Test
    @DisplayName("8. Business 404 does NOT count as infrastructure failure and does NOT open circuit")
    void testBusiness404_DoesNotOpenCircuit() {
        when(userFeignClient.getUserById(404))
                .thenThrow(new ResourceNotFoundException("User 404 not found"));

        for (int i = 0; i < 6; i++) {
            Optional<UserLookupDTO> result = userClient.findUserById(404);
            assertThat(result).isEmpty();
        }

        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        assertThat(circuitBreaker.getMetrics().getNumberOfFailedCalls()).isEqualTo(0);
    }

    @Test
    @DisplayName("9. Business 403 Access Denied is NOT retried")
    void testBusiness403_NotRetried() {
        when(userFeignClient.getUserByEmail("forbidden@test.com"))
                .thenThrow(new AccessDeniedException("Access denied"));

        assertThatThrownBy(() -> userClient.findUserByEmail("forbidden@test.com"))
                .isInstanceOf(AccessDeniedException.class);

        // Exactly 1 invocation - no retry
        verify(userFeignClient, times(1)).getUserByEmail("forbidden@test.com");
    }

    @Test
    @DisplayName("10. JWT propagation remains functional with FeignAuthInterceptor")
    void testJwtPropagation() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer test-resilience-token");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        FeignAuthInterceptor interceptor = new FeignAuthInterceptor();
        feign.RequestTemplate template = new feign.RequestTemplate();
        interceptor.apply(template);

        assertThat(template.headers().get(HttpHeaders.AUTHORIZATION))
                .isNotNull()
                .contains("Bearer test-resilience-token");
    }
}
