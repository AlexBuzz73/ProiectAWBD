package com.example.userservice;

import com.example.userservice.config.JwtConfig;
import com.example.userservice.domain.User;
import com.example.userservice.services.impl.JwtServiceImpl;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

import static org.assertj.core.api.Assertions.assertThat;

class UserJwtKeyReplicationTest {

    @Test
    @DisplayName("Multiple user-service instances loading shared PEM keys have identical JWKS and cross-verify tokens")
    void testMultipleInstancesShareIdenticalJwkAndCrossVerify(@TempDir Path tempDir) throws Exception {
        // 1. Generate test RSA key pair and write to shared temporary directory
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        KeyPair kp = kpg.generateKeyPair();
        RSAPrivateKey priv = (RSAPrivateKey) kp.getPrivate();
        RSAPublicKey pub = (RSAPublicKey) kp.getPublic();

        Path privFile = tempDir.resolve("private.pem");
        Path pubFile = tempDir.resolve("public.pem");
        Files.writeString(privFile, JwtConfig.toPem("PRIVATE KEY", priv.getEncoded()));
        Files.writeString(pubFile, JwtConfig.toPem("PUBLIC KEY", pub.getEncoded()));

        String keyId = "shared-cluster-key-test";

        // 2. Simulate User Service Instance A
        JwtConfig instanceA = new JwtConfig(keyId, privFile.toString(), pubFile.toString(), "", "");
        RSAKey rsaA = instanceA.rsaJwk();
        JwtEncoder encoderA = instanceA.jwtEncoder(instanceA.jwkSource());

        // 3. Simulate User Service Instance B
        JwtConfig instanceB = new JwtConfig(keyId, privFile.toString(), pubFile.toString(), "", "");
        RSAKey rsaB = instanceB.rsaJwk();
        JwtDecoder decoderB = instanceB.jwtDecoder();

        // 4. Assert JWKS equality
        assertThat(rsaA.getKeyID()).isEqualTo(rsaB.getKeyID());
        assertThat(rsaA.toRSAPublicKey()).isEqualTo(rsaB.toRSAPublicKey());

        JWKSet jwkSetA = new JWKSet(rsaA.toPublicJWK());
        JWKSet jwkSetB = new JWKSet(rsaB.toPublicJWK());
        assertThat(jwkSetA.toString()).isEqualTo(jwkSetB.toString());

        // 5. Assert cross-verification: Token issued by Instance A is valid for Instance B
        JwtServiceImpl jwtServiceA = new JwtServiceImpl(encoderA);
        User testUser = new User();
        testUser.setUserId(42);
        testUser.setEmail("shared.user@test.com");
        testUser.setUsername("shareduser");
        testUser.setRole("USER");

        String tokenFromA = jwtServiceA.generateToken(testUser);
        assertThat(tokenFromA).isNotBlank();

        Jwt decodedByB = decoderB.decode(tokenFromA);
        assertThat(decodedByB.getSubject()).isEqualTo("shared.user@test.com");
        assertThat(decodedByB.getClaimAsString("role")).isEqualTo("USER");
        assertThat(((Number) decodedByB.getClaims().get("userId")).intValue()).isEqualTo(42);
    }

    @Test
    @DisplayName("Direct PEM strings in properties load identical keys across replicas")
    void testDirectPemContentLoadsIdenticalKeys() throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        KeyPair kp = kpg.generateKeyPair();

        String privPem = JwtConfig.toPem("PRIVATE KEY", kp.getPrivate().getEncoded());
        String pubPem = JwtConfig.toPem("PUBLIC KEY", kp.getPublic().getEncoded());
        String keyId = "pem-string-key";

        JwtConfig replica1 = new JwtConfig(keyId, "", "", privPem, pubPem);
        JwtConfig replica2 = new JwtConfig(keyId, "", "", privPem, pubPem);

        assertThat(replica1.rsaJwk().getKeyID()).isEqualTo(replica2.rsaJwk().getKeyID());
        assertThat(replica1.rsaJwk().toRSAPublicKey()).isEqualTo(replica2.rsaJwk().toRSAPublicKey());
    }
}
