package com.example.accountservice.client;

import com.example.accountservice.dto.UserLookupDTO;
import com.example.accountservice.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserClient {

    private final UserFeignClient userFeignClient;

    public Optional<UserLookupDTO> findUserById(Integer userId) {
        try {
            return Optional.ofNullable(userFeignClient.getUserById(userId));
        } catch (ResourceNotFoundException e) {
            return Optional.empty();
        } catch (Exception e) {
            log.warn("Error calling user-service findUserById({}): {}", userId, e.getMessage());
            return Optional.empty();
        }
    }

    public Optional<UserLookupDTO> findUserByEmail(String email) {
        try {
            return Optional.ofNullable(userFeignClient.getUserByEmail(email));
        } catch (ResourceNotFoundException e) {
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
