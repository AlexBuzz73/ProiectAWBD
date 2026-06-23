package com.example.demo.controllers;

import com.example.demo.dto.IndividualRegistrationDTO;
import com.example.demo.dto.LoginRequestDTO;
import com.example.demo.dto.LoginResponseDTO;
import com.example.demo.dto.RegistrationRequestDTO;
import com.example.demo.services.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final AuthenticationManager authenticationManager;

    @PostMapping("/validate-individual")
    public ResponseEntity<Void> validateIndividual(@Valid @RequestBody IndividualRegistrationDTO dto) {
        authService.validateIndividualRegistrationData(dto);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/register")
    public ResponseEntity<Void> register(@Valid @RequestBody RegistrationRequestDTO dto) {
        authService.registerUser(dto.getIndividual(), dto.getUser());
        return ResponseEntity.ok().build();
    }

    /**
     * Login prin Spring Security AuthenticationManager (autentificare JDBC reala).
     * AuthService.login() pastreaza logica de business (numarare incercari esuate, blocare cont),
     * iar AuthenticationManager verifica credentialele prin CustomUserDetailsService + BCrypt.
     * La succes, sesiunea Spring Security e salvata astfel incat requesturile ulterioare
     * sunt autentificate automat prin cookie JSESSIONID.
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(
            @Valid @RequestBody LoginRequestDTO dto,
            HttpServletRequest request) {

        // Logica de business: contorizare incercari esuate, blocare cont dupa 3 greseli.
        // AuthService.login() aruncă IllegalArgumentException cu mesaj clar dacă ceva nu e ok.
        LoginResponseDTO response = authService.login(dto);

        // Autentificare reala prin Spring Security (JDBC via CustomUserDetailsService + BCrypt).
        // Daca autentificarea esueaza, AuthenticationManager arunca exceptii specifice.
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(dto.getEmail(), dto.getPassword())
            );

            // Salvam contextul de securitate in sesiunea HTTP, ca request-urile ulterioare
            // sa fie autentificate automat prin cookie-ul JSESSIONID.
            SecurityContextHolder.getContext().setAuthentication(authentication);
            new HttpSessionSecurityContextRepository()
                    .saveContext(SecurityContextHolder.getContext(), request, null);

            log.info("Spring Security: autentificare reusita pentru email={}", dto.getEmail());
        } catch (LockedException e) {
            throw new IllegalArgumentException("Contul este blocat. Contactati banca.");
        } catch (BadCredentialsException e) {
            // AuthService.login() a trecut deja, deci parola e corecta in BD.
            // Daca AuthenticationManager tot esueaza, e o problema de configurare.
            log.error("Spring Security: autentificare esuata desi AuthService a acceptat - email={}", dto.getEmail());
            throw new IllegalArgumentException("Eroare de autentificare. Incercati din nou.");
        }

        return ResponseEntity.ok(response);
    }
}
