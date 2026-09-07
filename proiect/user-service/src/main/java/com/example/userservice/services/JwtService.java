package com.example.userservice.services;

import com.example.userservice.domain.User;

public interface JwtService {
    String generateToken(User user);
    long getExpirationSeconds();
}
