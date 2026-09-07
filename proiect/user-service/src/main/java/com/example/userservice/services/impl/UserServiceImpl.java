package com.example.userservice.services.impl;

import com.example.userservice.domain.User;
import com.example.userservice.dto.UserLookupDTO;
import com.example.userservice.dto.UserResponseDTO;
import com.example.userservice.exceptions.ResourceNotFoundException;
import com.example.userservice.mappers.UserMapper;
import com.example.userservice.repositories.UserRepository;
import com.example.userservice.services.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Override
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Utilizatorul nu a fost gasit"));
    }

    @Override
    public User getUserById(Integer userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilizatorul nu a fost gasit"));
    }

    @Override
    public UserResponseDTO getCurrentUserProfile(String email) {
        User user = getUserByEmail(email);
        if (!"ACTIVE".equals(user.getStatus())) {
            throw new AccessDeniedException("Utilizator inactiv");
        }
        return userMapper.toUserResponseDTO(user);
    }

    @Override
    public UserLookupDTO getUserLookupById(Integer userId) {
        User user = getUserById(userId);
        return userMapper.toUserLookupDTO(user);
    }

    @Override
    public UserLookupDTO getUserLookupByEmail(String email) {
        User user = getUserByEmail(email);
        return userMapper.toUserLookupDTO(user);
    }
}
