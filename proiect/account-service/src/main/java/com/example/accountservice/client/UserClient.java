package com.example.accountservice.client;

import com.example.accountservice.dto.UserLookupDTO;
import com.example.accountservice.exceptions.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.Optional;

@Slf4j
@Component
public class UserClient {

    private final RestClient restClient;

    public UserClient(@Value("${user-service.url:http://localhost:8081}") String userServiceUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(userServiceUrl)
                .requestInitializer(request -> {
                    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                    if (auth instanceof JwtAuthenticationToken jwtAuth) {
                        request.getHeaders().setBearerAuth(jwtAuth.getToken().getTokenValue());
                    }
                })
                .build();
    }

    public Optional<UserLookupDTO> findUserById(Integer userId) {
        try {
            UserLookupDTO dto = restClient.get()
                    .uri("/api/users/{id}", userId)
                    .retrieve()
                    .onStatus(status -> status.value() == 404, (req, resp) -> {
                        // Handled by catch
                    })
                    .body(UserLookupDTO.class);
            return Optional.ofNullable(dto);
        } catch (HttpClientErrorException.NotFound e) {
            return Optional.empty();
        } catch (Exception e) {
            log.warn("Error calling user-service findUserById({}): {}", userId, e.getMessage());
            return Optional.empty();
        }
    }

    public Optional<UserLookupDTO> findUserByEmail(String email) {
        try {
            UserLookupDTO dto = restClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/api/users/by-email").queryParam("email", email).build())
                    .retrieve()
                    .onStatus(status -> status.value() == 404, (req, resp) -> {
                        // Handled by catch
                    })
                    .body(UserLookupDTO.class);
            return Optional.ofNullable(dto);
        } catch (HttpClientErrorException.NotFound e) {
            return Optional.empty();
        } catch (Exception e) {
            log.warn("Error calling user-service findUserByEmail({}): {}", email, e.getMessage());
            return Optional.empty();
        }
    }

    public UserLookupDTO requireUserByEmail(String email) {
        return findUserByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Utilizatorul cu email-ul " + email + " nu a fost gasit"));
    }

    public UserLookupDTO requireUserById(Integer userId) {
        return findUserById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilizatorul cu id-ul " + userId + " nu a fost gasit"));
    }
}
