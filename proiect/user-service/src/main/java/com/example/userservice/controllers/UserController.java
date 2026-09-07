package com.example.userservice.controllers;

import com.example.userservice.dto.UserLookupDTO;
import com.example.userservice.dto.UserResponseDTO;
import com.example.userservice.services.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<UserResponseDTO> getCurrentUser(@AuthenticationPrincipal Jwt jwt) {
        String email = jwt.getSubject();
        UserResponseDTO profile = userService.getCurrentUserProfile(email);
        return ResponseEntity.ok(profile);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserLookupDTO> getUserById(@PathVariable("id") Integer id) {
        UserLookupDTO lookup = userService.getUserLookupById(id);
        return ResponseEntity.ok(lookup);
    }

    @GetMapping("/by-email")
    public ResponseEntity<UserLookupDTO> getUserByEmail(@RequestParam("email") String email) {
        UserLookupDTO lookup = userService.getUserLookupByEmail(email);
        return ResponseEntity.ok(lookup);
    }
}
