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
