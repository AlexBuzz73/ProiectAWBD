package com.example.userservice.controllers;

import com.example.userservice.services.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AuthService authService;

    @PutMapping("/users/{userId}/unlock")
    public ResponseEntity<Void> unlockUser(@PathVariable("userId") int userId) {
        authService.unlockUser(userId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/unlock-user")
    public ResponseEntity<Void> unlockUserByEmail(@RequestParam("email") String email) {
        authService.unlockUserByEmail(email);
        return ResponseEntity.ok().build();
    }
}
