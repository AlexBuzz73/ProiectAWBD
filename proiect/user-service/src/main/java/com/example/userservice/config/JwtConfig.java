package com.example.userservice.config;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Slf4j
@Configuration
public class JwtConfig {

    private final RSAKey rsaJwk;

    public JwtConfig(
            @Value("${jwt.rsa.key-id:user-service-rsa-key-1}") String keyId,
            @Value("${jwt.rsa.private-key-path:}") String privateKeyPath,
            @Value("${jwt.rsa.public-key-path:}") String publicKeyPath,
            @Value("${jwt.rsa.private-key:}") String privateKeyContent,
            @Value("${jwt.rsa.public-key:}") String publicKeyContent) {
        this.rsaJwk = resolveRsaJwk(keyId, privateKeyPath, publicKeyPath, privateKeyContent, publicKeyContent);
        log.info("Initialized RSA signing key with keyID: {}", this.rsaJwk.getKeyID());
    }

    @Bean
    public RSAKey rsaJwk() {
        return this.rsaJwk;
    }

    public static RSAKey resolveRsaJwk(String keyId, String privPath, String pubPath, String privContent, String pubContent) {
        // 1. Direct PEM content in properties/env
        if (privContent != null && !privContent.isBlank() && pubContent != null && !pubContent.isBlank()) {
            try {
                RSAPrivateKey privKey = parsePrivateKey(privContent);
                RSAPublicKey pubKey = parsePublicKey(pubContent);
                return new RSAKey.Builder(pubKey).privateKey(privKey).keyID(keyId).build();
            } catch (Exception e) {
                log.error("Failed to parse RSA keys from properties content: {}", e.getMessage());
            }
        }

        // 2. Custom file paths in properties/env
        if (privPath != null && !privPath.isBlank() && pubPath != null && !pubPath.isBlank()) {
            try {
                Path prp = Paths.get(privPath);
                Path pup = Paths.get(pubPath);
                if (Files.exists(prp) && Files.exists(pup)) {
                    RSAPrivateKey privKey = parsePrivateKey(Files.readString(prp));
                    RSAPublicKey pubKey = parsePublicKey(Files.readString(pup));
                    return new RSAKey.Builder(pubKey).privateKey(privKey).keyID(keyId).build();
                }
            } catch (Exception e) {
                log.error("Failed to load RSA keys from paths '{}', '{}': {}", privPath, pubPath, e.getMessage());
            }
        }

        // 3. Shared file-based key pair (e.g. in shared ./keys directory) for multi-instance replication
        Path defaultKeysDir = Paths.get("keys");
        Path defaultPrivPath = defaultKeysDir.resolve("private.pem");
        Path defaultPubPath = defaultKeysDir.resolve("public.pem");
        try {
            if (Files.exists(defaultPrivPath) && Files.exists(defaultPubPath)) {
                log.info("Loading existing shared RSA keypair from {}", defaultKeysDir.toAbsolutePath());
                RSAPrivateKey privKey = parsePrivateKey(Files.readString(defaultPrivPath));
                RSAPublicKey pubKey = parsePublicKey(Files.readString(defaultPubPath));
                return new RSAKey.Builder(pubKey).privateKey(privKey).keyID(keyId).build();
            }

            // Create shared keypair if not exists
            Files.createDirectories(defaultKeysDir);
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
            kpg.initialize(2048);
            KeyPair kp = kpg.generateKeyPair();
            RSAPrivateKey privKey = (RSAPrivateKey) kp.getPrivate();
            RSAPublicKey pubKey = (RSAPublicKey) kp.getPublic();

            String privPem = toPem("PRIVATE KEY", privKey.getEncoded());
            String pubPem = toPem("PUBLIC KEY", pubKey.getEncoded());

            try {
                Files.writeString(defaultPrivPath, privPem, StandardCharsets.UTF_8, java.nio.file.StandardOpenOption.CREATE_NEW);
                Files.writeString(defaultPubPath, pubPem, StandardCharsets.UTF_8, java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.TRUNCATE_EXISTING);
                log.info("Generated and persisted new shared RSA keypair in {}", defaultKeysDir.toAbsolutePath());
                return new RSAKey.Builder(pubKey).privateKey(privKey).keyID(keyId).build();
            } catch (java.nio.file.FileAlreadyExistsException faee) {
                log.info("Shared RSA keypair was just generated by concurrent replica. Loading persisted keypair.");
                for (int i = 0; i < 10; i++) {
                    if (Files.exists(defaultPrivPath) && Files.exists(defaultPubPath)) {
                        try {
                            RSAPrivateKey loadedPriv = parsePrivateKey(Files.readString(defaultPrivPath));
                            RSAPublicKey loadedPub = parsePublicKey(Files.readString(defaultPubPath));
                            return new RSAKey.Builder(loadedPub).privateKey(loadedPriv).keyID(keyId).build();
                        } catch (Exception ignored) {
                            Thread.sleep(100);
                        }
                    }
                    Thread.sleep(100);
                }
                throw faee;
            }
        } catch (Exception e) {
            log.warn("Shared RSA keypair handling failed ({}), falling back to deterministic in-memory key", e.getMessage());
            return generateFallbackRsaJwk(keyId);
        }
    }

    public static RSAKey generateFallbackRsaJwk(String keyId) {
        try {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
            kpg.initialize(2048);
            KeyPair kp = kpg.generateKeyPair();
            return new RSAKey.Builder((RSAPublicKey) kp.getPublic())
                    .privateKey((RSAPrivateKey) kp.getPrivate())
                    .keyID(keyId)
                    .build();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Failed to generate fallback RSA key pair", e);
        }
    }

    public static RSAPrivateKey parsePrivateKey(String pem) throws Exception {
        String clean = pem.replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s+", "");
        byte[] decoded = Base64.getDecoder().decode(clean);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(decoded);
        return (RSAPrivateKey) KeyFactory.getInstance("RSA").generatePrivate(spec);
    }

    public static RSAPublicKey parsePublicKey(String pem) throws Exception {
        String clean = pem.replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s+", "");
        byte[] decoded = Base64.getDecoder().decode(clean);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(decoded);
        return (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(spec);
    }

    public static String toPem(String type, byte[] encoded) {
        String base64 = Base64.getMimeEncoder(64, new byte[]{'\n'}).encodeToString(encoded);
        return "-----BEGIN " + type + "-----\n" + base64 + "\n-----END " + type + "-----\n";
    }

    @Bean
    public JWKSource<SecurityContext> jwkSource() {
        JWKSet jwkSet = new JWKSet(rsaJwk);
        return new ImmutableJWKSet<>(jwkSet);
    }

    @Bean
    public JwtEncoder jwtEncoder(JWKSource<SecurityContext> jwkSource) {
        return new NimbusJwtEncoder(jwkSource);
    }

    @Bean
    public JwtDecoder jwtDecoder() throws Exception {
        return NimbusJwtDecoder.withPublicKey(rsaJwk.toRSAPublicKey()).build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();
        authoritiesConverter.setAuthorityPrefix("ROLE_");
        authoritiesConverter.setAuthoritiesClaimName("role");

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);
        return converter;
    }
}
