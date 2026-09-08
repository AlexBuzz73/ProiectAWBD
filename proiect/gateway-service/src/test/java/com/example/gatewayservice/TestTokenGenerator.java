package com.example.gatewayservice;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Component
public class TestTokenGenerator {

    @Autowired(required = false)
    private JwtEncoder jwtEncoder;

    public String generateToken(Integer userId, String username, String email, String role) {
        if (jwtEncoder == null) {
            return "mock-token";
        }
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("user-service")
                .issuedAt(now)
                .expiresAt(now.plus(1, ChronoUnit.HOURS))
                .subject(email)
                .claim("userId", userId)
                .claim("username", username)
                .claim("role", role)
                .build();
        return jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }
}
