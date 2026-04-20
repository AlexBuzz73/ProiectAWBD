package com.example.demo.services;

import com.example.demo.dto.IndividualRegistrationDTO;
import com.example.demo.dto.LoginRequestDTO;
import com.example.demo.dto.LoginResponseDTO;
import com.example.demo.dto.UserRegistrationDTO;

public interface AuthService {

    void validateIndividualRegistrationData(IndividualRegistrationDTO individualDto);
    void registerUser(IndividualRegistrationDTO individualDto, UserRegistrationDTO userDto);
    LoginResponseDTO login(LoginRequestDTO loginRequestDTO);
    void unlockUser(int userId);
}
