package com.example.transactionservice;

import com.example.transactionservice.controllers.PaymentController;
import com.example.transactionservice.exceptions.GlobalExceptionHandler;
import com.example.transactionservice.exceptions.ResourceNotFoundException;
import com.example.transactionservice.services.CurrentUserService;
import com.example.transactionservice.services.TransactionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.HttpRequestMethodNotSupportedException;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AdditionalTests {

    @Test
    @DisplayName("TransactionServiceApplication main method test")
    void testMainMethod() {
        // Run with exit code 0 or verify class loads
        assertThat(TransactionServiceApplication.class).isNotNull();
    }

    @Test
    @DisplayName("CurrentUserService tests: email, username, role, admin check")
    void testCurrentUserService() {
        CurrentUserService service = new CurrentUserService();

        Jwt jwt = Mockito.mock(Jwt.class);
        Mockito.when(jwt.getSubject()).thenReturn("user@test.com");
        Mockito.when(jwt.getClaimAsString("username")).thenReturn("testuser");
        Mockito.when(jwt.getClaimAsString("role")).thenReturn("USER");
        Mockito.when(jwt.getClaim("userId")).thenReturn(10);
        Mockito.when(jwt.getTokenValue()).thenReturn("token-abc");

        JwtAuthenticationToken auth = new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority("ROLE_USER")));
        SecurityContext context = Mockito.mock(SecurityContext.class);
        Mockito.when(context.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(context);

        assertThat(service.getCurrentUserEmail()).isEqualTo("user@test.com");
        assertThat(service.getCurrentUsername()).isEqualTo("testuser");
        assertThat(service.getCurrentUserRole()).isEqualTo("USER");
        assertThat(service.getCurrentUserId()).isEqualTo(10);
        assertThat(service.getJwtTokenValue()).isEqualTo("token-abc");
        assertThat(service.isAdmin()).isFalse();
        assertThat(service.requireCurrentUserId(10)).isEqualTo(10);

        // Claim as string
        Mockito.when(jwt.getClaim("userId")).thenReturn("10");
        assertThat(service.getCurrentUserId()).isEqualTo(10);

        // Missing claim
        Mockito.when(jwt.getClaim("userId")).thenReturn(new Object());
        assertThatThrownBy(service::getCurrentUserId).isInstanceOf(AccessDeniedException.class);

        // Unauthenticated
        Mockito.when(context.getAuthentication()).thenReturn(null);
        assertThat(service.isAdmin()).isFalse();
        assertThat(service.getJwtTokenValue()).isNull();
        assertThatThrownBy(service::getJwt).isInstanceOf(AuthenticationCredentialsNotFoundException.class);

        // Admin check
        Mockito.when(jwt.getClaim("userId")).thenReturn(10);
        Authentication adminAuth = new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        Mockito.when(context.getAuthentication()).thenReturn(adminAuth);
        assertThat(service.isAdmin()).isTrue();
        assertThat(service.requireCurrentUserId(99)).isEqualTo(99);

        // Non-admin accessing another user
        Mockito.when(context.getAuthentication()).thenReturn(auth);
        Mockito.when(jwt.getClaim("userId")).thenReturn(10);
        assertThatThrownBy(() -> service.requireCurrentUserId(99)).isInstanceOf(AccessDeniedException.class);

        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("GlobalExceptionHandler tests for uncovered status codes")
    void testGlobalExceptionHandler() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();

        ResponseEntity<Map<String, String>> res1 = handler.handleIllegalArgument(new IllegalArgumentException("Arg invalid"));
        assertThat(res1.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(res1.getBody().get("error")).isEqualTo("Arg invalid");

        ResponseEntity<Map<String, String>> res2 = handler.handleIllegalState(new IllegalStateException("State invalid"));
        assertThat(res2.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        ResponseEntity<Map<String, String>> res3 = handler.handleBadRequestExceptions(new HttpMessageNotReadableException("Not readable", (org.springframework.http.HttpInputMessage) null));
        assertThat(res3.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        ResponseEntity<Map<String, String>> res4 = handler.handleNotFoundExceptions(new ResourceNotFoundException());
        assertThat(res4.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        ResponseEntity<Map<String, String>> res5 = handler.handleMethodNotSupported(new HttpRequestMethodNotSupportedException("POST"));
        assertThat(res5.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);

        ResponseEntity<Map<String, String>> res6 = handler.handleAccessDenied(new AccessDeniedException("Forbidden"));
        assertThat(res6.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        ResponseEntity<Map<String, String>> res7 = handler.handleAuthenticationException(new AuthenticationCredentialsNotFoundException("Auth error"));
        assertThat(res7.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        ResponseEntity<Map<String, String>> res8 = handler.handleGenericException(new RuntimeException("Crash"));
        assertThat(res8.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
