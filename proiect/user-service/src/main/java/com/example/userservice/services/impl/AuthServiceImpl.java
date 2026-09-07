package com.example.userservice.services.impl;

import com.example.userservice.domain.Individual;
import com.example.userservice.domain.User;
import com.example.userservice.dto.IndividualRegistrationDTO;
import com.example.userservice.dto.LoginRequestDTO;
import com.example.userservice.dto.LoginResponseDTO;
import com.example.userservice.dto.UserRegistrationDTO;
import com.example.userservice.mappers.UserMapper;
import com.example.userservice.repositories.IndividualRepository;
import com.example.userservice.repositories.UserRepository;
import com.example.userservice.services.AuthService;
import com.example.userservice.services.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Calendar;
import java.util.Date;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final IndividualRepository individualRepository;
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Override
    public void validateIndividualRegistrationData(IndividualRegistrationDTO individualDto) {
        validateIndividualRequiredFields(individualDto);
        validateUniqueCnp(individualDto.getCnp());
        validateMinimumAge(individualDto.getDateOfBirth());
    }

    private void validateIndividualRequiredFields(IndividualRegistrationDTO individualDto) {
        if (individualDto.getFirstName() == null || individualDto.getFirstName().trim().isEmpty()) {
            throw new IllegalArgumentException("First name is required!");
        }
        if (individualDto.getLastName() == null || individualDto.getLastName().trim().isEmpty()) {
            throw new IllegalArgumentException("Last name is required!");
        }
        if (individualDto.getCnp() == null || individualDto.getCnp().trim().isEmpty()) {
            throw new IllegalArgumentException("CNP is required!");
        }
        if (individualDto.getPhoneNumber() == null || individualDto.getPhoneNumber().trim().isEmpty()) {
            throw new IllegalArgumentException("Phone number is required!");
        }
        if (individualDto.getDateOfBirth() == null) {
            throw new IllegalArgumentException("Birth date is required!");
        }
    }

    private void validateUniqueCnp(String cnp) {
        if (individualRepository.existsByCnp(cnp)) {
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
    @Transactional
    public void registerUser(IndividualRegistrationDTO individualDto, UserRegistrationDTO userDto) {
        validateIndividualRegistrationData(individualDto);
        validateUserRegistrationData(userDto);

        Individual individual = userMapper.toIndividual(individualDto);
        Individual savedIndividual = individualRepository.save(individual);

        User user = userMapper.toUser(userDto);
        user.setPasswordHash(passwordEncoder.encode(userDto.getPassword()));
        user.setRole("USER");
        user.setStatus("ACTIVE");
        user.setFailedLoginAttempts(0);
        user.setCreatedAt(new Date());
        user.setUpdatedAt(new Date());
        user.setIndividual(savedIndividual);
        userRepository.save(user);

        log.info("user-service: registered new user: userId={}, email={}", user.getUserId(), user.getEmail());
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
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("This username was already chosen!");
        }
    }

    private void validateUniqueEmail(String email) {
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("This email already exists!");
        }
    }

    private void validatePassword(String password) {
        if (password.length() < 8) {
            throw new IllegalArgumentException("The password must be at least 8 characters long!");
        }
    }

    @Override
    @Transactional
    public LoginResponseDTO login(LoginRequestDTO loginRequestDTO) {
        User user = userRepository.findByEmail(loginRequestDTO.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password!"));

        if ("BLOCKED".equals(user.getStatus())) {
            log.warn("Login attempt on blocked account: email={}", loginRequestDTO.getEmail());
            throw new IllegalArgumentException("Your account is blocked! Please contact the bank!");
        } else if ("CLOSED".equals(user.getStatus())) {
            throw new IllegalArgumentException("Your account is closed!");
        }

        boolean passwordMatches = passwordEncoder.matches(loginRequestDTO.getPassword(), user.getPasswordHash());
        if (!passwordMatches) {
            handleFailedLogin(user);
            throw new IllegalArgumentException("Invalid email or password!");
        }

        user.setFailedLoginAttempts(0);
        user.setUpdatedAt(new Date());
        userRepository.save(user);

        String token = jwtService.generateToken(user);
        log.info("Login successful in user-service: userId={}, email={}", user.getUserId(), user.getEmail());

        return new LoginResponseDTO(
                token,
                "Bearer",
                jwtService.getExpirationSeconds(),
                user.getUserId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole()
        );
    }

    private void handleFailedLogin(User user) {
        int attempts = user.getFailedLoginAttempts() + 1;
        user.setFailedLoginAttempts(attempts);

        if (attempts >= 3) {
            user.setStatus("BLOCKED");
            log.warn("Account blocked after {} failed attempts: email={}", attempts, user.getEmail());
        } else {
            log.warn("Failed login attempt ({}/3): email={}", attempts, user.getEmail());
        }

        user.setUpdatedAt(new Date());
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void unlockUser(int userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found!"));

        user.setStatus("ACTIVE");
        user.setFailedLoginAttempts(0);
        user.setUpdatedAt(new Date());
        userRepository.save(user);

        log.info("Admin: user unlocked: userId={}", userId);
    }

    @Override
    @Transactional
    public void unlockUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User with email " + email + " not found!"));

        user.setStatus("ACTIVE");
        user.setFailedLoginAttempts(0);
        user.setUpdatedAt(new Date());
        userRepository.save(user);

        log.info("Admin: user unlocked: email={}", email);
    }
}
