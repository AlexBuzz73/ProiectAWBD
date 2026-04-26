package com.example.demo.controllers;

import com.example.demo.services.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AuthService authService;

    @PutMapping("/users/{userId}/unlock")
    public ResponseEntity<Void> unlockUser(@PathVariable int userId) {
        authService.unlockUser(userId);
        return ResponseEntity.ok().build();
    }
}
