package com.example.demo.config;

import com.example.demo.domain.User;
import com.example.demo.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Leaga Spring Security de tabela `users` din baza de date (autentificare JDBC prin JPA).
 * Spring Security apeleaza loadUserByUsername la orice incercare de autentificare prin
 * AuthenticationManager, in loc sa foloseasca userul in-memory generat automat.
 *
 * Rolurile din BD ("USER", "ADMIN") sunt prefixate cu "ROLE_" conform conventiei Spring Security,
 * astfel incat hasRole("USER") si hasRole("ADMIN") functioneaza corect in SecurityConfig.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("Spring Security: utilizator negasit pentru email={}", email);
                    return new UsernameNotFoundException("Utilizatorul nu a fost gasit: " + email);
                });

        log.debug("Spring Security: incarcare user pentru autentificare - email={}, rol={}, status={}",
                email, user.getRole(), user.getStatus());

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(user.getPasswordHash())
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole())))
                .accountLocked("BLOCKED".equals(user.getStatus()))
                .disabled("CLOSED".equals(user.getStatus()))
                .build();
    }
}
