package com.example.demo.services;

import com.example.demo.domain.User;
import com.example.demo.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CurrentUserService {
    private final UserRepository userRepository;

    public User getCurrentUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            throw new AuthenticationCredentialsNotFoundException("Autentificare necesara");
        }
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new AccessDeniedException("Utilizator indisponibil"));
        if (!"ACTIVE".equals(user.getStatus())) {
            throw new AccessDeniedException("Utilizator inactiv");
        }
        return user;
    }

    public Integer getCurrentUserId() {
        return getCurrentUser().getUserId();
    }

    /** Legacy IDs are assertions only; the returned identity always comes from the session. */
    public Integer requireCurrentUserId(Integer requestedId) {
        Integer currentId = getCurrentUserId();
        if (requestedId != null && !currentId.equals(requestedId)) {
            throw new AccessDeniedException("Acces interzis la datele altui utilizator");
        }
        return currentId;
    }
}
