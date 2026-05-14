package com.example.demo.services;

import com.example.demo.domain.BankLimit;
import com.example.demo.domain.User;
import com.example.demo.domain.UserLimit;
import com.example.demo.dto.BankLimitRequestDTO;
import com.example.demo.dto.BankLimitResponseDTO;
import com.example.demo.dto.UserLimitRequestDTO;
import com.example.demo.dto.UserLimitResponseDTO;
import com.example.demo.mappers.LimitMapper;
import com.example.demo.repositories.BankLimitRepository;
import com.example.demo.repositories.UserLimitRepository;
import com.example.demo.repositories.UserRepository;
import com.example.demo.services.impl.LimitServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LimitServiceImplTest {

    @Mock
    private BankLimitRepository bankLimitRepository;

    @Mock
    private UserLimitRepository userLimitRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private LimitMapper limitMapper;

    @InjectMocks
    private LimitServiceImpl limitService;

    private BankLimit bankLimit;
    private User user;
    private UserLimit userLimit;

    @BeforeEach
    void setUp() {
        bankLimit = new BankLimit();
        bankLimit.setBankLimitId(1);
        bankLimit.setMaxAmountPerTransactionRon(new BigDecimal("5000"));
        bankLimit.setMaxDailyAmountRon(new BigDecimal("20000"));
        bankLimit.setMaxDailyTransactionsCount(new BigDecimal("10"));
        bankLimit.setStatus("ACTIVE");

        user = new User();
        user.setUserId(1);
        user.setUsername("testuser");
        user.setStatus("ACTIVE");

        userLimit = new UserLimit();
        userLimit.setUserLimitId(1);
        userLimit.setUser(user);
        userLimit.setMaxAmountPerTransactionRon(new BigDecimal("3000"));
        userLimit.setMaxDailyAmountRon(new BigDecimal("10000"));
        userLimit.setMaxDailyTransactionsCount(new BigDecimal("5"));
        userLimit.setStatus("ACTIVE");
    }

    @Test
    void getBankLimits_shouldReturnBankLimitResponse_whenActiveBankLimitExists() {
        BankLimitResponseDTO responseDTO = new BankLimitResponseDTO(
                1,
                new BigDecimal("5000"),
                new BigDecimal("20000"),
                new BigDecimal("10"),
                "ACTIVE"
        );

        when(bankLimitRepository.findByStatus("ACTIVE")).thenReturn(Optional.of(bankLimit));
        when(limitMapper.toBankLimitResponseDTO(bankLimit)).thenReturn(responseDTO);

        BankLimitResponseDTO result = limitService.getBankLimits();

        assertNotNull(result);
        assertEquals("ACTIVE", result.getStatus());
        assertEquals(new BigDecimal("5000"), result.getMaxAmountPerTransactionRon());

        verify(bankLimitRepository).findByStatus("ACTIVE");
        verify(limitMapper).toBankLimitResponseDTO(bankLimit);
    }

    @Test
    void getBankLimits_shouldThrowException_whenNoActiveBankLimitExists() {
        when(bankLimitRepository.findByStatus("ACTIVE")).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> limitService.getBankLimits()
        );

        assertEquals("Active bank limits not found!", exception.getMessage());
    }

    @Test
    void updateBankLimits_shouldUpdateAndReturnResponse() {
        BankLimitRequestDTO requestDTO = new BankLimitRequestDTO(
                new BigDecimal("6000"),
                new BigDecimal("25000"),
                new BigDecimal("12")
        );

        BankLimitResponseDTO responseDTO = new BankLimitResponseDTO(
                1,
                new BigDecimal("6000"),
                new BigDecimal("25000"),
                new BigDecimal("12"),
                "ACTIVE"
        );

        when(bankLimitRepository.findByStatus("ACTIVE")).thenReturn(Optional.of(bankLimit));
        when(bankLimitRepository.save(bankLimit)).thenReturn(bankLimit);
        when(limitMapper.toBankLimitResponseDTO(bankLimit)).thenReturn(responseDTO);

        BankLimitResponseDTO result = limitService.updateBankLimits(requestDTO);

        assertNotNull(result);
        assertEquals(new BigDecimal("6000"), bankLimit.getMaxAmountPerTransactionRon());
        assertEquals(new BigDecimal("25000"), bankLimit.getMaxDailyAmountRon());
        assertEquals(new BigDecimal("12"), bankLimit.getMaxDailyTransactionsCount());

        verify(bankLimitRepository).save(bankLimit);
    }

    @Test
    void getUserLimits_shouldReturnUserLimitResponse_whenUserLimitExists() {
        UserLimitResponseDTO responseDTO = new UserLimitResponseDTO(
                1,
                new BigDecimal("3000"),
                new BigDecimal("10000"),
                new BigDecimal("5"),
                "ACTIVE"
        );

        when(userLimitRepository.findByUserUserIdAndStatus(1, "ACTIVE"))
                .thenReturn(Optional.of(userLimit));
        when(limitMapper.toUserLimitResponseDTO(userLimit)).thenReturn(responseDTO);

        UserLimitResponseDTO result = limitService.getUserLimits(1);

        assertNotNull(result);
        assertEquals("ACTIVE", result.getStatus());

        verify(userLimitRepository).findByUserUserIdAndStatus(1, "ACTIVE");
    }

    @Test
    void getUserLimits_shouldReturnEmptyResponse_whenUserLimitDoesNotExist() {
        UserLimitResponseDTO emptyResponse = new UserLimitResponseDTO();

        when(userLimitRepository.findByUserUserIdAndStatus(1, "ACTIVE"))
                .thenReturn(Optional.empty());
        when(limitMapper.toEmptyUserLimitResponseDTO()).thenReturn(emptyResponse);

        UserLimitResponseDTO result = limitService.getUserLimits(1);

        assertNotNull(result);

        verify(limitMapper).toEmptyUserLimitResponseDTO();
    }

    @Test
    void updateUserLimits_shouldCreateNewUserLimit_whenUserLimitDoesNotExist() {
        UserLimitRequestDTO requestDTO = new UserLimitRequestDTO(
                new BigDecimal("3000"),
                new BigDecimal("10000"),
                new BigDecimal("5")
        );

        UserLimitResponseDTO responseDTO = new UserLimitResponseDTO(
                1,
                new BigDecimal("3000"),
                new BigDecimal("10000"),
                new BigDecimal("5"),
                "ACTIVE"
        );

        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(userLimitRepository.findByUserUserIdAndStatus(1, "ACTIVE"))
                .thenReturn(Optional.empty());
        when(userLimitRepository.save(any(UserLimit.class))).thenReturn(userLimit);
        when(limitMapper.toUserLimitResponseDTO(userLimit)).thenReturn(responseDTO);

        UserLimitResponseDTO result = limitService.updateUserLimits(1, requestDTO);

        assertNotNull(result);
        assertEquals("ACTIVE", result.getStatus());

        verify(userLimitRepository).save(any(UserLimit.class));
    }

    @Test
    void updateUserLimits_shouldUpdateExistingUserLimit_whenUserLimitExists() {
        UserLimitRequestDTO requestDTO = new UserLimitRequestDTO(
                new BigDecimal("3500"),
                new BigDecimal("12000"),
                new BigDecimal("6")
        );

        UserLimitResponseDTO responseDTO = new UserLimitResponseDTO(
                1,
                new BigDecimal("3500"),
                new BigDecimal("12000"),
                new BigDecimal("6"),
                "ACTIVE"
        );

        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(userLimitRepository.findByUserUserIdAndStatus(1, "ACTIVE"))
                .thenReturn(Optional.of(userLimit));
        when(userLimitRepository.save(userLimit)).thenReturn(userLimit);
        when(limitMapper.toUserLimitResponseDTO(userLimit)).thenReturn(responseDTO);

        UserLimitResponseDTO result = limitService.updateUserLimits(1, requestDTO);

        assertNotNull(result);
        assertEquals(new BigDecimal("3500"), userLimit.getMaxAmountPerTransactionRon());
        assertEquals(new BigDecimal("12000"), userLimit.getMaxDailyAmountRon());
        assertEquals(new BigDecimal("6"), userLimit.getMaxDailyTransactionsCount());

        verify(userLimitRepository).save(userLimit);
    }

    @Test
    void updateUserLimits_shouldThrowException_whenUserNotFound() {
        UserLimitRequestDTO requestDTO = new UserLimitRequestDTO(
                new BigDecimal("3000"),
                new BigDecimal("10000"),
                new BigDecimal("5")
        );

        when(userRepository.findById(1)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> limitService.updateUserLimits(1, requestDTO)
        );

        assertEquals("User not found!", exception.getMessage());
    }

    @Test
    void updateUserLimits_shouldThrowException_whenUserInactive() {
        user.setStatus("BLOCKED");

        UserLimitRequestDTO requestDTO = new UserLimitRequestDTO(
                new BigDecimal("3000"),
                new BigDecimal("10000"),
                new BigDecimal("5")
        );

        when(userRepository.findById(1)).thenReturn(Optional.of(user));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> limitService.updateUserLimits(1, requestDTO)
        );

        assertEquals("User: User is not active!", exception.getMessage());
    }

    @Test
    void deleteUserLimits_shouldSetStatusInactive_whenUserLimitExists() {
        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(userLimitRepository.findByUserUserIdAndStatus(1, "ACTIVE"))
                .thenReturn(Optional.of(userLimit));

        limitService.deleteUserLimits(1);

        assertEquals("INACTIVE", userLimit.getStatus());
    }

    @Test
    void validateRequiredFields_shouldThrowException_whenUserAmountIsNull() {
        UserLimitRequestDTO userDTO = new UserLimitRequestDTO(
                null,
                new BigDecimal("10000"),
                new BigDecimal("5")
        );

        BankLimitRequestDTO bankDTO = new BankLimitRequestDTO(
                new BigDecimal("5000"),
                new BigDecimal("20000"),
                new BigDecimal("10")
        );

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> limitService.validateRequiredFields(userDTO, bankDTO)
        );

        assertEquals("User: Max Amount Per Transaction in Ron is required!", exception.getMessage());
    }

    @Test
    void validatePositiveValues_shouldThrowException_whenUserAmountIsNegative() {
        UserLimitRequestDTO userDTO = new UserLimitRequestDTO(
                new BigDecimal("-1"),
                new BigDecimal("10000"),
                new BigDecimal("5")
        );

        BankLimitRequestDTO bankDTO = new BankLimitRequestDTO(
                new BigDecimal("5000"),
                new BigDecimal("20000"),
                new BigDecimal("10")
        );

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> limitService.validatePositiveValues(userDTO, bankDTO)
        );

        assertEquals("User: Max Amount Per Transaction in Ron must be greater than zero!", exception.getMessage());
    }
}