package com.example.userservice.services;

import com.example.userservice.domain.User;
import com.example.userservice.dto.UserLookupDTO;
import com.example.userservice.dto.UserResponseDTO;

public interface UserService {
    User getUserByEmail(String email);
    User getUserById(Integer userId);
    UserResponseDTO getCurrentUserProfile(String email);
    UserLookupDTO getUserLookupById(Integer userId);
    UserLookupDTO getUserLookupByEmail(String email);
}
