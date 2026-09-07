package com.example.userservice.services.impl;

import com.example.userservice.domain.User;
import com.example.userservice.services.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class JwtServiceImpl implements JwtService {

    private static final long EXPIRATION_SECONDS = 7200; // 2 hours
    private final JwtEncoder jwtEncoder;

    @Override
    public String generateToken(User user) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("user-service")
                .issuedAt(now)
                .expiresAt(now.plus(EXPIRATION_SECONDS, ChronoUnit.SECONDS))
                .subject(user.getEmail())
                .claim("userId", user.getUserId())
                .claim("username", user.getUsername())
                .claim("role", user.getRole())
                .build();

        JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256).build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        log.debug("Issued JWT for user: userId={}, email={}, role={}", user.getUserId(), user.getEmail(), user.getRole());
        return token;
    }

    @Override
    public long getExpirationSeconds() {
        return EXPIRATION_SECONDS;
    }
}
