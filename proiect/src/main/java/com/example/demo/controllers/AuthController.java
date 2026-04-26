package com.example.demo.controllers;

import com.example.demo.dto.IndividualRegistrationDTO;
import com.example.demo.dto.LoginRequestDTO;
import com.example.demo.dto.LoginResponseDTO;
import com.example.demo.dto.RegistrationRequestDTO;
import com.example.demo.services.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/validate-individual")
    public ResponseEntity<Void> validateIndividual(@RequestBody IndividualRegistrationDTO dto) {
        authService.validateIndividualRegistrationData(dto);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/register")
    public ResponseEntity<Void> register(@RequestBody RegistrationRequestDTO dto) {
        authService.registerUser(dto.getIndividual(), dto.getUser());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@RequestBody LoginRequestDTO dto) {
        LoginResponseDTO response = authService.login(dto);
        return ResponseEntity.ok(response);
    }
}
