package com.example.userservice.services;

import com.example.userservice.dto.IndividualRegistrationDTO;
import com.example.userservice.dto.LoginRequestDTO;
import com.example.userservice.dto.LoginResponseDTO;
import com.example.userservice.dto.UserRegistrationDTO;

public interface AuthService {
    void validateIndividualRegistrationData(IndividualRegistrationDTO individualDto);
    void registerUser(IndividualRegistrationDTO individualDto, UserRegistrationDTO userDto);
    LoginResponseDTO login(LoginRequestDTO loginRequestDTO);
    void unlockUser(int userId);
    void unlockUserByEmail(String email);
}
