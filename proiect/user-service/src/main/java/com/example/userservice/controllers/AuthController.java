package com.example.userservice.controllers;

import com.example.userservice.dto.IndividualRegistrationDTO;
import com.example.userservice.dto.LoginRequestDTO;
import com.example.userservice.dto.LoginResponseDTO;
import com.example.userservice.dto.RegistrationRequestDTO;
import com.example.userservice.services.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/validate-individual")
    public ResponseEntity<Void> validateIndividual(@Valid @RequestBody IndividualRegistrationDTO dto) {
        authService.validateIndividualRegistrationData(dto);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/register")
    public ResponseEntity<Void> register(@Valid @RequestBody RegistrationRequestDTO dto) {
        authService.registerUser(dto.getIndividual(), dto.getUser());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@Valid @RequestBody LoginRequestDTO dto) {
        LoginResponseDTO response = authService.login(dto);
        return ResponseEntity.ok(response);
    }
}
