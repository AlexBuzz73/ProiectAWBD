package com.example.accountservice;

import com.example.accountservice.client.CustomFeignErrorDecoder;
import com.example.accountservice.client.FeignAuthInterceptor;
import com.example.accountservice.client.UserClient;
import com.example.accountservice.client.UserFeignClient;
import com.example.accountservice.dto.UserLookupDTO;
import com.example.accountservice.exceptions.ResourceNotFoundException;
import com.example.accountservice.exceptions.ServiceUnavailableException;
import feign.Request;
import feign.RequestTemplate;
import feign.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserClientTest {

    @Mock
    private UserFeignClient userFeignClient;

    private UserClient userClient;
    private FeignAuthInterceptor authInterceptor;
    private CustomFeignErrorDecoder errorDecoder;

    @BeforeEach
    void setUp() {
        userClient = new UserClient(userFeignClient);
        authInterceptor = new FeignAuthInterceptor();
        errorDecoder = new CustomFeignErrorDecoder();
        RequestContextHolder.resetRequestAttributes();
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("findUserById returns UserLookupDTO on 200 OK")
    void testFindUserByIdSuccess() {
        UserLookupDTO expected = new UserLookupDTO(101, "ion", "ion@test.com", "USER", true);
        when(userFeignClient.getUserById(101)).thenReturn(expected);

        Optional<UserLookupDTO> result = userClient.findUserById(101);
        assertThat(result).isPresent();
        assertThat(result.get().getUsername()).isEqualTo("ion");
    }

    @Test
    @DisplayName("findUserById returns empty Optional on ResourceNotFoundException")
    void testFindUserByIdNotFound() {
        when(userFeignClient.getUserById(999)).thenThrow(new ResourceNotFoundException("Not found"));

        Optional<UserLookupDTO> result = userClient.findUserById(999);
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findUserByEmail returns UserLookupDTO on 200 OK")
    void testFindUserByEmailSuccess() {
        UserLookupDTO expected = new UserLookupDTO(102, "maria", "maria@test.com", "USER", true);
        when(userFeignClient.getUserByEmail("maria@test.com")).thenReturn(expected);

        Optional<UserLookupDTO> result = userClient.findUserByEmail("maria@test.com");
        assertThat(result).isPresent();
        assertThat(result.get().getUsername()).isEqualTo("maria");
    }

    @Test
    @DisplayName("findUserByEmail returns empty Optional on ResourceNotFoundException")
    void testFindUserByEmailNotFound() {
        when(userFeignClient.getUserByEmail("unknown@test.com")).thenThrow(new ResourceNotFoundException("Not found"));

        Optional<UserLookupDTO> result = userClient.findUserByEmail("unknown@test.com");
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("requireUserById returns DTO or throws ResourceNotFoundException")
    void testRequireUserById() {
        UserLookupDTO expected = new UserLookupDTO(103, "dan", "dan@test.com", "USER", true);
        when(userFeignClient.getUserById(103)).thenReturn(expected);
        when(userFeignClient.getUserById(404)).thenThrow(new ResourceNotFoundException("Not found"));

        assertThat(userClient.requireUserById(103).getUsername()).isEqualTo("dan");
        assertThatThrownBy(() -> userClient.requireUserById(404))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("requireUserByEmail returns DTO or throws ResourceNotFoundException")
    void testRequireUserByEmail() {
        UserLookupDTO expected = new UserLookupDTO(104, "ana", "ana@test.com", "USER", true);
        when(userFeignClient.getUserByEmail("ana@test.com")).thenReturn(expected);
        when(userFeignClient.getUserByEmail("absent@test.com")).thenThrow(new ResourceNotFoundException("Not found"));

        assertThat(userClient.requireUserByEmail("ana@test.com").getUsername()).isEqualTo("ana");
        assertThatThrownBy(() -> userClient.requireUserByEmail("absent@test.com"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("FeignAuthInterceptor propagates Authorization from current HttpServletRequest")
    void testFeignAuthInterceptorFromHttpRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer test-http-token-12345");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        RequestTemplate template = new RequestTemplate();
        authInterceptor.apply(template);

        Collection<String> authHeaders = template.headers().get(HttpHeaders.AUTHORIZATION);
        assertThat(authHeaders).isNotNull().contains("Bearer test-http-token-12345");
    }

    @Test
    @DisplayName("FeignAuthInterceptor propagates Bearer token from SecurityContext fallback")
    void testFeignAuthInterceptorFromSecurityContext() {
        Jwt jwt = Jwt.withTokenValue("jwt-security-token-abcde")
                .header("alg", "RS256")
                .claim("sub", "user@test.com")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
        JwtAuthenticationToken authToken = new JwtAuthenticationToken(jwt);
        SecurityContextHolder.getContext().setAuthentication(authToken);

        RequestTemplate template = new RequestTemplate();
        authInterceptor.apply(template);

        Collection<String> authHeaders = template.headers().get(HttpHeaders.AUTHORIZATION);
        assertThat(authHeaders).isNotNull().contains("Bearer jwt-security-token-abcde");
    }

    @Test
    @DisplayName("CustomFeignErrorDecoder maps HTTP 404 to ResourceNotFoundException")
    void testErrorDecoder404() {
        Response response = Response.builder()
                .status(404)
                .reason("Not Found")
                .request(Request.create(Request.HttpMethod.GET, "/api/users/1", Collections.emptyMap(), null, StandardCharsets.UTF_8, null))
                .headers(Collections.emptyMap())
                .build();

        Exception ex = errorDecoder.decode("getUserById", response);
        assertThat(ex).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("CustomFeignErrorDecoder maps HTTP 403 to AccessDeniedException")
    void testErrorDecoder403() {
        Response response = Response.builder()
                .status(403)
                .reason("Forbidden")
                .request(Request.create(Request.HttpMethod.GET, "/api/users/1", Collections.emptyMap(), null, StandardCharsets.UTF_8, null))
                .headers(Collections.emptyMap())
                .build();

        Exception ex = errorDecoder.decode("getUserById", response);
        assertThat(ex).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("CustomFeignErrorDecoder maps HTTP 400 to IllegalArgumentException")
    void testErrorDecoder400() {
        Response response = Response.builder()
                .status(400)
                .reason("Bad Request")
                .request(Request.create(Request.HttpMethod.GET, "/api/users/1", Collections.emptyMap(), null, StandardCharsets.UTF_8, null))
                .headers(Collections.emptyMap())
                .build();

        Exception ex = errorDecoder.decode("getUserById", response);
        assertThat(ex).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("CustomFeignErrorDecoder maps HTTP 503 to ServiceUnavailableException")
    void testErrorDecoder503() {
        Response response = Response.builder()
                .status(503)
                .reason("Service Unavailable")
                .request(Request.create(Request.HttpMethod.GET, "/api/users/1", Collections.emptyMap(), null, StandardCharsets.UTF_8, null))
                .headers(Collections.emptyMap())
                .build();

        Exception ex = errorDecoder.decode("getUserById", response);
        assertThat(ex).isInstanceOf(ServiceUnavailableException.class);
    }
}
