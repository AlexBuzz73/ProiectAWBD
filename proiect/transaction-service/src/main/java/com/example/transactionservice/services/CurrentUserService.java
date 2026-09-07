package com.example.transactionservice.services;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;

@Service
public class CurrentUserService {

    public Integer getCurrentUserId() {
        Jwt jwt = getJwt();
        Object claim = jwt.getClaim("userId");
        if (claim instanceof Number number) {
            return number.intValue();
        } else if (claim instanceof String str) {
            return Integer.parseInt(str);
        }
        throw new AccessDeniedException("Token-ul JWT nu contine claim-ul 'userId'");
    }

    public String getCurrentUserEmail() {
        return getJwt().getSubject();
    }

    public String getCurrentUsername() {
        String username = getJwt().getClaimAsString("username");
        return username != null ? username : getCurrentUserEmail();
    }

    public String getCurrentUserRole() {
        return getJwt().getClaimAsString("role");
    }

    public boolean isAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;
        return auth.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }

    public Integer requireCurrentUserId(Integer requestedId) {
        Integer currentId = getCurrentUserId();
        if (requestedId != null && !currentId.equals(requestedId)) {
            if (!isAdmin()) {
                throw new AccessDeniedException("Acces interzis la datele altui utilizator");
            }
            return requestedId;
        }
        return currentId;
    }

    public Jwt getJwt() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || !(authentication instanceof JwtAuthenticationToken jwtAuth)) {
            throw new AuthenticationCredentialsNotFoundException("Autentificare necesara");
        }
        return jwtAuth.getToken();
    }

    public String getJwtTokenValue() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            return jwtAuth.getToken().getTokenValue();
        }
        return null;
    }
}
