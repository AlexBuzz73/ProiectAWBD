package com.example.accountservice.client;

import com.example.accountservice.dto.UserLookupDTO;
import com.example.accountservice.exceptions.ResourceNotFoundException;
import com.example.accountservice.exceptions.ServiceUnavailableException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserClient {

    private final UserFeignClient userFeignClient;

    @CircuitBreaker(name = "userServiceCircuitBreaker", fallbackMethod = "findUserByIdFallback")
    @Retry(name = "userServiceRetry")
    public Optional<UserLookupDTO> findUserById(Integer userId) {
        try {
            return Optional.ofNullable(userFeignClient.getUserById(userId));
        } catch (ResourceNotFoundException e) {
            return Optional.empty();
        }
    }

    public Optional<UserLookupDTO> findUserByIdFallback(Integer userId, Throwable t) {
        log.error("Resilience fallback for findUserById({}): {}", userId, t.getMessage());
        if (t instanceof ResourceNotFoundException) {
            return Optional.empty();
        }
        if (t instanceof org.springframework.security.access.AccessDeniedException ade) {
            throw ade;
        }
        if (t instanceof IllegalArgumentException iae) {
            throw iae;
        }
        throw new ServiceUnavailableException("Serviciul de utilizatori nu este disponibil momentan.");
    }

    @CircuitBreaker(name = "userServiceCircuitBreaker", fallbackMethod = "findUserByEmailFallback")
    @Retry(name = "userServiceRetry")
    public Optional<UserLookupDTO> findUserByEmail(String email) {
        try {
            return Optional.ofNullable(userFeignClient.getUserByEmail(email));
        } catch (ResourceNotFoundException e) {
            return Optional.empty();
        }
    }

    public Optional<UserLookupDTO> findUserByEmailFallback(String email, Throwable t) {
        log.error("Resilience fallback for findUserByEmail({}): {}", email, t.getMessage());
        if (t instanceof ResourceNotFoundException) {
            return Optional.empty();
        }
        if (t instanceof org.springframework.security.access.AccessDeniedException ade) {
            throw ade;
        }
        if (t instanceof IllegalArgumentException iae) {
            throw iae;
        }
        throw new ServiceUnavailableException("Serviciul de utilizatori nu este disponibil momentan.");
    }

    public UserLookupDTO requireUserByEmail(String email) {
        return findUserByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Utilizatorul cu email-ul " + email + " nu a fost gasit"));
    }

    public UserLookupDTO requireUserById(Integer userId) {
        return findUserById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilizatorul cu id-ul " + userId + " nu a fost gasit"));
    }

    @CircuitBreaker(name = "userServiceCircuitBreaker", fallbackMethod = "getInstanceInfoFallback")
    @Retry(name = "userServiceRetry")
    public Map<String, Object> getInstanceInfo() {
        return userFeignClient.getInstanceInfo();
    }

    public Map<String, Object> getInstanceInfoFallback(Throwable t) {
        log.error("Resilience fallback for getInstanceInfo(): {}", t.getMessage());
        throw new ServiceUnavailableException("Serviciul de utilizatori nu este disponibil momentan.");
    }
}
