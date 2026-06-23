package com.example.demo.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.Customizer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Autentificare JDBC prin DaoAuthenticationProvider:
     * conecteaza Spring Security la UserDetailsService-ul nostru (tabela users din BD)
     * si la BCryptPasswordEncoder pentru verificarea parolei.
     */
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    /**
     * AuthenticationManager expus ca Bean, necesar in AuthController
     * pentru autentificarea programatica prin /api/auth/login.
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(Customizer.withDefaults())
            // CSRF dezactivat pentru SPA React (requests vin cu JSON, nu cu form HTML).
            // Activarea CSRF pentru SPA necesita trimiterea token-ului XSRF-TOKEN la fiecare
            // request mutant (POST/PUT/DELETE) - lasam dezactivat, e acceptabil pentru SPA.
            .csrf(csrf -> csrf.disable())
            .authenticationProvider(authenticationProvider())
            .authorizeHttpRequests(auth -> auth
                // Public - nu necesita autentificare
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/h2-console/**").permitAll()

                // Doar ADMIN
                .requestMatchers("/api/admin/**").hasRole("ADMIN")

                // USER sau ADMIN (orice utilizator autentificat)
                .requestMatchers("/api/accounts/**").hasAnyRole("USER", "ADMIN")
                .requestMatchers("/api/transactions/**").hasAnyRole("USER", "ADMIN")
                .requestMatchers("/api/payments/**").hasAnyRole("USER", "ADMIN")
                .requestMatchers("/api/users/**").hasAnyRole("USER", "ADMIN")

                // Orice alt endpoint necesita autentificare
                .anyRequest().authenticated()
            )
            // Sesiune standard (ALWAYS) - necesara pentru a mentine autentificarea
            // intre request-uri in cadrul SPA-ului, fara JWT.
            .sessionManagement(session -> session
                .sessionCreationPolicy(
                    org.springframework.security.config.http.SessionCreationPolicy.IF_REQUIRED
                )
            )
            // Logout: invalideaza sesiunea si cookie-ul JSESSIONID.
            // Frontend-ul apeleaza POST /api/auth/logout, care este delegat automat
            // de Spring Security la mecanismul de logout configurat aici.
            .logout(logout -> logout
                .logoutUrl("/api/auth/logout")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .logoutSuccessHandler((request, response, authentication) -> {
                    response.setStatus(200);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"message\": \"Logout successful\"}");
                })
            )
            // Dezactivam redirect-ul automat catre /login al Spring Security:
            // frontul gestioneaza redirectarea catre LoginPage.jsx.
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setStatus(401);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"error\": \"Autentificare necesara\"}");
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    response.setStatus(403);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"error\": \"Acces interzis\"}");
                })
            )
            .headers(headers -> headers.frameOptions(frame -> frame.disable()));

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:5173"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        // Necesar pentru a trimite cookie-ul de sesiune (JSESSIONID) intre React si Spring
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
