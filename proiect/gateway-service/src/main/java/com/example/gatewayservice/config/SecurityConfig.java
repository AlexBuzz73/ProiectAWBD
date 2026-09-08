package com.example.gatewayservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
public class SecurityConfig {

    @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri:http://localhost:8081/.well-known/jwks.json}")
    private String jwkSetUri;

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(
            ServerHttpSecurity http,
            ReactiveJwtDecoder reactiveJwtDecoder,
            Converter<Jwt, Mono<AbstractAuthenticationToken>> reactiveJwtAuthenticationConverter) {
        http
                .cors(ServerHttpSecurity.CorsSpec::disable)
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .pathMatchers("/actuator/**").permitAll()
                        .pathMatchers("/api/auth/**").permitAll()
                        .pathMatchers("/.well-known/jwks.json").permitAll()
                        .pathMatchers("/api/admin/**").hasRole("ADMIN")
                        .pathMatchers("/api/**").authenticated()
                        .anyExchange().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .jwtDecoder(reactiveJwtDecoder)
                                .jwtAuthenticationConverter(reactiveJwtAuthenticationConverter)
                        )
                        .authenticationEntryPoint((exchange, ex) -> {
                            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                            exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
                            String cid = exchange.getRequest().getHeaders().getFirst("X-Correlation-Id");
                            if (cid != null) {
                                exchange.getResponse().getHeaders().set("X-Correlation-Id", cid);
                            }
                            exchange.getResponse().getHeaders().set("X-Gateway-Service", "gateway-service");
                            byte[] bytes = "{\"error\":\"Unauthorized\",\"message\":\"Token-ul JWT lipseste sau este invalid.\",\"status\":401}".getBytes(StandardCharsets.UTF_8);
                            return exchange.getResponse().writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(bytes)));
                        })
                        .accessDeniedHandler((exchange, denied) -> {
                            exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                            exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
                            String cid = exchange.getRequest().getHeaders().getFirst("X-Correlation-Id");
                            if (cid != null) {
                                exchange.getResponse().getHeaders().set("X-Correlation-Id", cid);
                            }
                            exchange.getResponse().getHeaders().set("X-Gateway-Service", "gateway-service");
                            byte[] bytes = "{\"error\":\"Forbidden\",\"message\":\"Nu aveti permisiunea de a accesa aceasta resursa.\",\"status\":403}".getBytes(StandardCharsets.UTF_8);
                            return exchange.getResponse().writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(bytes)));
                        })
                );

        return http.build();
    }

    @Bean
    @ConditionalOnMissingBean(ReactiveJwtDecoder.class)
    public ReactiveJwtDecoder reactiveJwtDecoder() {
        return NimbusReactiveJwtDecoder.withJwkSetUri(jwkSetUri).build();
    }

    @Bean
    public Converter<Jwt, Mono<AbstractAuthenticationToken>> reactiveJwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter grantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
        grantedAuthoritiesConverter.setAuthorityPrefix("ROLE_");
        grantedAuthoritiesConverter.setAuthoritiesClaimName("role");

        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(grantedAuthoritiesConverter);

        return new ReactiveJwtAuthenticationConverterAdapter(jwtAuthenticationConverter);
    }
}
