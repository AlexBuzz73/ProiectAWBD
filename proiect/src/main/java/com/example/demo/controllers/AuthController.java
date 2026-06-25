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

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(
            @Valid @RequestBody LoginRequestDTO dto,
            HttpServletRequest request) {


        LoginResponseDTO response = authService.login(dto);


        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(dto.getEmail(), dto.getPassword())
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);
            new HttpSessionSecurityContextRepository()
                    .saveContext(SecurityContextHolder.getContext(), request, null);

            log.info("Spring Security: autentificare reusita pentru email={}", dto.getEmail());
        } catch (LockedException e) {
            throw new IllegalArgumentException("Contul este blocat. Contactati banca.");
        } catch (BadCredentialsException e) {

            log.error("Spring Security: autentificare esuata desi AuthService a acceptat - email={}", dto.getEmail());
            throw new IllegalArgumentException("Eroare de autentificare. Incercati din nou.");
        }

        return ResponseEntity.ok(response);
    }
}
