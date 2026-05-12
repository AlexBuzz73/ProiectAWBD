package com.example.demo.services;

import com.example.demo.domain.Individual;
import com.example.demo.domain.User;
import com.example.demo.dto.IndividualRegistrationDTO;
import com.example.demo.dto.LoginRequestDTO;
import com.example.demo.dto.LoginResponseDTO;
import com.example.demo.dto.UserRegistrationDTO;
import com.example.demo.mappers.AuthMapper;
import com.example.demo.repositories.IndividualRepository;
import com.example.demo.repositories.UserRepository;
import com.example.demo.services.impl.AuthServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Calendar;
import java.util.Date;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private IndividualRepository individualRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthMapper authMapper;

    @InjectMocks
    private AuthServiceImpl authService;

    private Date adultBirthDate() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.YEAR, -20);
        return calendar.getTime();
    }

    private Date under18BirthDate() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.YEAR, -17);
        return calendar.getTime();
    }

    private IndividualRegistrationDTO validIndividualDto() {
        IndividualRegistrationDTO dto = new IndividualRegistrationDTO();
        dto.setFirstName("Alex");
        dto.setLastName("Buzatu");
        dto.setCnp("1234567890123");
        dto.setPhoneNumber("0712345678");
        dto.setDateOfBirth(adultBirthDate());
        return dto;
    }

    private UserRegistrationDTO validUserDto() {
        UserRegistrationDTO dto = new UserRegistrationDTO();
        dto.setUsername("alex123");
        dto.setEmail("alex@test.com");
        dto.setPassword("password123");
        return dto;
    }

    private LoginRequestDTO validLoginRequest() {
        LoginRequestDTO request = new LoginRequestDTO();
        request.setEmail("alex@test.com");
        request.setPassword("password123");
        return request;
    }

    private User validUser() {
        User user = new User();
        user.setUserId(1);
        user.setUsername("alex123");
        user.setEmail("alex@test.com");
        user.setPasswordHash("hashedPassword");
        user.setRole("USER");
        user.setStatus("ACTIVE");
        user.setFailedLoginAttempts(2);
        return user;
    }

    private LoginResponseDTO validLoginResponseDTO() {
        LoginResponseDTO responseDTO = new LoginResponseDTO();
        responseDTO.setUserId(1);
        responseDTO.setUsername("alex123");
        responseDTO.setEmail("alex@test.com");
        responseDTO.setRole("USER");
        return responseDTO;
    }


    @Test
    void shouldValidateIndividual_whenDataIsValid() {
        IndividualRegistrationDTO dto = validIndividualDto();
        assertDoesNotThrow(() -> authService.validateIndividualRegistrationData(dto));
    }

    @Test
    void shouldThrowException_whenFirstNameIsMissing() {
        IndividualRegistrationDTO dto = validIndividualDto();
        dto.setFirstName("");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> authService.validateIndividualRegistrationData(dto));
        assertEquals("First name is required!", exception.getMessage());
    }

    @Test
    void shouldThrowException_whenLastNameIsMissing() {
        IndividualRegistrationDTO dto = validIndividualDto();
        dto.setLastName("");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> authService.validateIndividualRegistrationData(dto));
        assertEquals("Last name is required!", exception.getMessage());
    }

    @Test
    void shouldThrowException_whenCnpIsMissing() {
        IndividualRegistrationDTO dto = validIndividualDto();
        dto.setCnp("");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> authService.validateIndividualRegistrationData(dto));
        assertEquals("CNP is required!", exception.getMessage());
    }

    @Test
    void shouldThrowException_whenPhoneNumberIsMissing() {
        IndividualRegistrationDTO dto = validIndividualDto();
        dto.setPhoneNumber("");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> authService.validateIndividualRegistrationData(dto));
        assertEquals("Phone number is required!", exception.getMessage());
    }

    @Test
    void shouldThrowException_whenDateOfBirthIsMissing() {
        IndividualRegistrationDTO dto = validIndividualDto();
        dto.setDateOfBirth(null);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> authService.validateIndividualRegistrationData(dto));
        assertEquals("Birth date is required!", exception.getMessage());
    }

    @Test
    void shouldThrowException_whenCnpAlreadyExists() {
        IndividualRegistrationDTO dto = validIndividualDto();

        when(individualRepository.existsByCnp(dto.getCnp()))
                .thenReturn(true);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> authService.validateIndividualRegistrationData(dto));
        assertEquals("An individual with this CNP already exists!", exception.getMessage());
    }

    @Test
    void shouldThrowException_whenUserIsUnder18() {
        IndividualRegistrationDTO dto = validIndividualDto();
        dto.setDateOfBirth(under18BirthDate());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> authService.validateIndividualRegistrationData(dto));
        assertEquals("You must be at least 18 years old to enroll!", exception.getMessage());
    }


    @Test
    void shouldRegisterUserSuccessfully() {
        IndividualRegistrationDTO individualDto = validIndividualDto();
        UserRegistrationDTO userDto = validUserDto();

        Individual individual = new Individual();
        individual.setIndividualId(1);
        individual.setCnp(individualDto.getCnp());

        User user = new User();
        user.setUsername(userDto.getUsername());
        user.setEmail(userDto.getEmail());

        when(individualRepository.existsByCnp(individualDto.getCnp()))
                .thenReturn(false);

        when(userRepository.existsByEmail(userDto.getEmail()))
                .thenReturn(false);

        when(userRepository.existsByUsername(userDto.getUsername()))
                .thenReturn(false);

        when(authMapper.toIndividual(individualDto))
                .thenReturn(individual);

        when(individualRepository.save(individual))
                .thenReturn(individual);

        when(authMapper.toUser(userDto))
                .thenReturn(user);

        when(passwordEncoder.encode(userDto.getPassword()))
                .thenReturn("hashedPassword");

        authService.registerUser(individualDto, userDto);

        assertEquals("hashedPassword", user.getPasswordHash());
        assertEquals("USER", user.getRole());
        assertEquals("ACTIVE", user.getStatus());
        assertEquals(0, user.getFailedLoginAttempts());
        assertEquals(individual, user.getIndividual());

        verify(individualRepository, times(1)).save(individual);
        verify(userRepository, times(1)).save(user);
        verify(passwordEncoder, times(1)).encode(userDto.getPassword());
    }

    @Test
    void shouldThrowException_whenRegisterWithExistingCnp() {
        IndividualRegistrationDTO individualDto = validIndividualDto();
        UserRegistrationDTO userDto = validUserDto();

        when(individualRepository.existsByCnp(individualDto.getCnp()))
                .thenReturn(true);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> authService.registerUser(individualDto, userDto));
        assertEquals("An individual with this CNP already exists!", exception.getMessage());

        verify(individualRepository, never()).save(any(Individual.class));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void shouldThrowException_whenRegisterWithExistingEmail() {
        IndividualRegistrationDTO individualDto = validIndividualDto();
        UserRegistrationDTO userDto = validUserDto();

        when(individualRepository.existsByCnp(individualDto.getCnp()))
                .thenReturn(false);

        when(userRepository.existsByEmail(userDto.getEmail()))
                .thenReturn(true);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> authService.registerUser(individualDto, userDto));
        assertEquals("This email already exists!", exception.getMessage());

        verify(individualRepository, never()).save(any(Individual.class));
        verify(userRepository, never()).save(any(User.class));
    }


    @Test
    void shouldLoginSuccessfully_whenCredentialsAreCorrect() {
        LoginRequestDTO request = validLoginRequest();
        User user = validUser();
        LoginResponseDTO responseDTO = validLoginResponseDTO();

        when(userRepository.findByEmail(request.getEmail()))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches(request.getPassword(), user.getPasswordHash()))
                .thenReturn(true);

        when(authMapper.toLoginResponseDTO(user))
                .thenReturn(responseDTO);

        LoginResponseDTO response = authService.login(request);

        assertNotNull(response);
        assertEquals(1, response.getUserId());
        assertEquals("alex123", response.getUsername());
        assertEquals("alex@test.com", response.getEmail());
        assertEquals("USER", response.getRole());
        assertEquals(0, user.getFailedLoginAttempts());

        verify(userRepository, times(1)).save(user);
        verify(authMapper, times(1)).toLoginResponseDTO(user);
    }

    @Test
    void shouldThrowException_whenEmailDoesNotExist() {
        LoginRequestDTO request = validLoginRequest();

        when(userRepository.findByEmail(request.getEmail()))
                .thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> authService.login(request));
        assertEquals("Invalid email or password!", exception.getMessage());

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void shouldThrowException_whenUserIsBlocked() {
        LoginRequestDTO request = validLoginRequest();
        User user = validUser();
        user.setStatus("BLOCKED");

        when(userRepository.findByEmail(request.getEmail()))
                .thenReturn(Optional.of(user));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> authService.login(request));
        assertEquals("Your account is blocked! Please contact the bank!", exception.getMessage());

        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void shouldThrowException_whenUserIsClosed() {
        LoginRequestDTO request = validLoginRequest();
        User user = validUser();
        user.setStatus("CLOSED");

        when(userRepository.findByEmail(request.getEmail()))
                .thenReturn(Optional.of(user));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> authService.login(request));
        assertEquals("Your account is closed!", exception.getMessage());

        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void shouldThrowException_whenPasswordIsIncorrect() {
        LoginRequestDTO request = validLoginRequest();
        User user = validUser();
        user.setFailedLoginAttempts(0);

        when(userRepository.findByEmail(request.getEmail()))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches(request.getPassword(), user.getPasswordHash()))
                .thenReturn(false);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> authService.login(request));
        assertEquals("Invalid email or password!", exception.getMessage());
        assertEquals(1, user.getFailedLoginAttempts());
        assertEquals("ACTIVE", user.getStatus());

        verify(userRepository, times(1)).save(user);
    }

    @Test
    void shouldBlockUser_whenFailedAttemptsReachThree() {
        LoginRequestDTO request = validLoginRequest();
        User user = validUser();
        user.setFailedLoginAttempts(2);

        when(userRepository.findByEmail(request.getEmail()))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches(request.getPassword(), user.getPasswordHash()))
                .thenReturn(false);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> authService.login(request));
        assertEquals("Invalid email or password!", exception.getMessage());
        assertEquals(3, user.getFailedLoginAttempts());
        assertEquals("BLOCKED", user.getStatus());

        verify(userRepository, times(1)).save(user);
    }


    @Test
    void shouldUnlockUserSuccessfully() {
        User user = validUser();
        user.setStatus("BLOCKED");
        user.setFailedLoginAttempts(3);

        when(userRepository.findById(user.getUserId()))
                .thenReturn(Optional.of(user));

        authService.unlockUser(user.getUserId());

        assertEquals("ACTIVE", user.getStatus());
        assertEquals(0, user.getFailedLoginAttempts());

        verify(userRepository, times(1)).save(user);
    }

    @Test
    void shouldThrowException_whenUnlockingNonExistingUser() {
        when(userRepository.findById(999))
                .thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> authService.unlockUser(999));
        assertEquals("User not found!", exception.getMessage());

        verify(userRepository, never()).save(any(User.class));
    }
}
