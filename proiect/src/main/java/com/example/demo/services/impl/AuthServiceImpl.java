package com.example.demo.services.impl;

import com.example.demo.domain.Individual;
import com.example.demo.domain.User;
import com.example.demo.dto.IndividualRegistrationDTO;
import com.example.demo.dto.LoginRequestDTO;
import com.example.demo.dto.LoginResponseDTO;
import com.example.demo.dto.UserRegistrationDTO;
import com.example.demo.mappers.AuthMapper;
import com.example.demo.repositories.IndividualRepository;
import com.example.demo.repositories.UserRepository;
import com.example.demo.services.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Calendar;
import java.util.Date;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final IndividualRepository individualRepository;
    private final UserRepository userRepository;
    private final AuthMapper authMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void validateIndividualRegistrationData(IndividualRegistrationDTO individualDto) {
        validateIndividualRequiredFields(individualDto);
        validateUniqueCnp(individualDto.getCnp());
        validateMinimumAge(individualDto.getDateOfBirth());
    }

    private void validateIndividualRequiredFields(IndividualRegistrationDTO individualDto) {
        if(individualDto.getFirstName() == null || individualDto.getFirstName().trim().isEmpty()) {
            throw new IllegalArgumentException("First name is required!");
        }

        if(individualDto.getLastName() == null || individualDto.getLastName().trim().isEmpty()) {
            throw new IllegalArgumentException("Last name is required!");
        }

        if(individualDto.getCnp() == null || individualDto.getCnp().trim().isEmpty()) {
            throw new IllegalArgumentException("CNP is required!");
        }

        if(individualDto.getPhoneNumber() == null || individualDto.getPhoneNumber().trim().isEmpty()) {
            throw new IllegalArgumentException("Phone number is required!");
        }

        if(individualDto.getDateOfBirth() == null) {
            throw new IllegalArgumentException("Birth date is required!");
        }
    }

    private void validateUniqueCnp(String cnp) {
        if(individualRepository.existsByCnp(cnp)) {
            throw new IllegalArgumentException("An individual with this CNP already exists!");
        }
    }

    private void validateMinimumAge(Date dateOfBirth) {
        Calendar today = Calendar.getInstance();
        Calendar birthDate = Calendar.getInstance();
        birthDate.setTime(dateOfBirth);

        int age = today.get(Calendar.YEAR) - birthDate.get(Calendar.YEAR);
        if (today.get(Calendar.DAY_OF_YEAR) < birthDate.get(Calendar.DAY_OF_YEAR)) {
            age--;
        }

        if (age < 18) {
            throw new IllegalArgumentException("You must be at least 18 years old to enroll!");
        }
    }

    @Override
    public void registerUser(IndividualRegistrationDTO individualDto, UserRegistrationDTO userDto) {
        validateIndividualRegistrationData(individualDto);
        validateUserRegistrationData(userDto);

        Individual individual = authMapper.toIndividual(individualDto);
        Individual savedIndividual = individualRepository.save(individual);

        User user = authMapper.toUser(userDto);
        user.setPasswordHash(passwordEncoder.encode(userDto.getPassword()));
        user.setRole("USER");
        user.setStatus("ACTIVE");
        user.setFailedLoginAttempts(0);
        user.setCreatedAt(new Date());
        user.setUpdatedAt(new Date());
        user.setIndividual(savedIndividual);
        userRepository.save(user);
    }

    private void validateUserRegistrationData(UserRegistrationDTO userDto) {
        validateUserRequiredFields(userDto);
        validateUniqueEmail(userDto.getEmail());
        validateUniqueUsername(userDto.getUsername());
        validatePassword(userDto.getPassword());
    }

    private void validateUserRequiredFields(UserRegistrationDTO userDto) {
        if (userDto.getUsername() == null || userDto.getUsername().trim().isEmpty()) {
            throw new IllegalArgumentException("Username is required!");
        }

        if (userDto.getEmail() == null || userDto.getEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("Email is required!");
        }

        if (userDto.getPassword() == null || userDto.getPassword().trim().isEmpty()) {
            throw new IllegalArgumentException("Password is required!");
        }
    }

    private void validateUniqueUsername(String username) {
        if(userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("This username was already chosen!");
        }
    }

    private void validateUniqueEmail(String email) {
        if(userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("This email already exists!");
        }
    }

    private void validatePassword(String password) {
        if(password.length() < 8) {
            throw new IllegalArgumentException("The password must be at least 8 characters long!");
        }
    }

    @Override
    public LoginResponseDTO login(LoginRequestDTO loginRequestDTO) {
        User user = userRepository.findByEmail(loginRequestDTO.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password!"));

        if ("BLOCKED".equals(user.getStatus())) {
            throw new IllegalArgumentException("Your account is blocked! Please contact the bank!");
        }
        else if ("CLOSED".equals(user.getStatus())) {
            throw new IllegalArgumentException("Your account is closed!");
        }

        boolean passwordMatches = passwordEncoder.matches(loginRequestDTO.getPassword(), user.getPasswordHash());
        if(!passwordMatches) {
            handleFailedLogin(user);
            throw new IllegalArgumentException("Invalid email or password!");
        }

        user.setFailedLoginAttempts(0);
        user.setUpdatedAt(new Date());
        userRepository.save(user);

        return authMapper.toLoginResponseDTO(user);
    }

    private void handleFailedLogin(User user) {
        int attempts = user.getFailedLoginAttempts() + 1;
        user.setFailedLoginAttempts(attempts);

        if (attempts >= 3) {
            user.setStatus("BLOCKED");
        }

        user.setUpdatedAt(new Date());
        userRepository.save(user);
    }

    @Override
    public void unlockUser(int userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found!"));

        user.setStatus("ACTIVE");
        user.setFailedLoginAttempts(0);
        user.setUpdatedAt(new Date());

        userRepository.save(user);
    }
}
