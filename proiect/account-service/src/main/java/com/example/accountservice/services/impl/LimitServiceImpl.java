package com.example.accountservice.services.impl;

import com.example.accountservice.domain.BankLimit;
import com.example.accountservice.domain.UserLimit;
import com.example.accountservice.dto.BankLimitRequestDTO;
import com.example.accountservice.dto.BankLimitResponseDTO;
import com.example.accountservice.dto.UserLimitRequestDTO;
import com.example.accountservice.dto.UserLimitResponseDTO;
import com.example.accountservice.exceptions.ResourceNotFoundException;
import com.example.accountservice.mappers.LimitMapper;
import com.example.accountservice.repositories.BankLimitRepository;
import com.example.accountservice.repositories.UserLimitRepository;
import com.example.accountservice.services.LimitService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class LimitServiceImpl implements LimitService {

    private final BankLimitRepository bankLimitRepository;
    private final UserLimitRepository userLimitRepository;
    private final LimitMapper limitMapper;

    @Override
    @Transactional
    public BankLimitResponseDTO getBankLimits() {
        BankLimit bankLimit = getOrCreateActiveBankLimit();
        return limitMapper.toBankLimitResponseDTO(bankLimit);
    }

    @Override
    @Transactional
    public BankLimitResponseDTO updateBankLimits(BankLimitRequestDTO dto) {
        BankLimit bankLimit = getOrCreateActiveBankLimit();
        bankLimit.setMaxAmountPerTransactionRon(dto.getMaxAmountPerTransactionRon());
        bankLimit.setMaxDailyAmountRon(dto.getMaxDailyAmountRon());
        bankLimit.setMaxDailyTransactionsCount(dto.getMaxDailyTransactionsCount());
        bankLimit.setUpdatedAt(new Date());

        BankLimit saved = bankLimitRepository.save(bankLimit);
        log.info("Updated bank limits: tx={}, daily={}, count={}",
                saved.getMaxAmountPerTransactionRon(), saved.getMaxDailyAmountRon(), saved.getMaxDailyTransactionsCount());
        return limitMapper.toBankLimitResponseDTO(saved);
    }

    @Override
    @Transactional
    public void deleteBankLimits(Integer bankLimitId) {
        if (!bankLimitRepository.existsById(bankLimitId)) {
            throw new ResourceNotFoundException("Limita bancara cu id " + bankLimitId + " nu a fost gasita");
        }
        bankLimitRepository.deleteById(bankLimitId);
        log.info("Deleted bank limit id {}", bankLimitId);
    }

    @Override
    @Transactional
    public UserLimitResponseDTO getUserLimits(Integer userId) {
        Optional<UserLimit> optionalUserLimit = userLimitRepository.findByUserIdAndStatus(userId, "ACTIVE");
        if (optionalUserLimit.isPresent()) {
            return limitMapper.toUserLimitResponseDTO(optionalUserLimit.get());
        }

        // Fallback to bank limits
        BankLimit bankLimit = getOrCreateActiveBankLimit();
        UserLimitResponseDTO fallback = new UserLimitResponseDTO();
        fallback.setUserLimitId(0);
        fallback.setMaxAmountPerTransactionRon(bankLimit.getMaxAmountPerTransactionRon());
        fallback.setMaxDailyAmountRon(bankLimit.getMaxDailyAmountRon());
        fallback.setMaxDailyTransactionsCount(bankLimit.getMaxDailyTransactionsCount());
        fallback.setStatus("ACTIVE");
        return fallback;
    }

    @Override
    @Transactional
    public UserLimitResponseDTO updateUserLimits(Integer userId, UserLimitRequestDTO dto) {
        BankLimit bankLimit = getOrCreateActiveBankLimit();

        if (dto.getMaxAmountPerTransactionRon().compareTo(bankLimit.getMaxAmountPerTransactionRon()) > 0) {
            throw new IllegalArgumentException("Limita per tranzactie depaseste limita bancii (" + bankLimit.getMaxAmountPerTransactionRon() + " RON)");
        }
        if (dto.getMaxDailyAmountRon().compareTo(bankLimit.getMaxDailyAmountRon()) > 0) {
            throw new IllegalArgumentException("Limita zilnica depaseste limita bancii (" + bankLimit.getMaxDailyAmountRon() + " RON)");
        }
        if (dto.getMaxDailyTransactionsCount().compareTo(bankLimit.getMaxDailyTransactionsCount()) > 0) {
            throw new IllegalArgumentException("Numarul maxim de tranzactii zilnice depaseste limita bancii (" + bankLimit.getMaxDailyTransactionsCount() + ")");
        }

        UserLimit userLimit = userLimitRepository.findByUserId(userId).orElseGet(() -> {
            UserLimit ul = new UserLimit();
            ul.setUserId(userId);
            ul.setStatus("ACTIVE");
            ul.setCreatedAt(new Date());
            return ul;
        });

        userLimit.setStatus("ACTIVE");
        userLimit.setMaxAmountPerTransactionRon(dto.getMaxAmountPerTransactionRon());
        userLimit.setMaxDailyAmountRon(dto.getMaxDailyAmountRon());
        userLimit.setMaxDailyTransactionsCount(dto.getMaxDailyTransactionsCount());
        userLimit.setUpdatedAt(new Date());

        UserLimit saved = userLimitRepository.save(userLimit);
        log.info("Updated user limits for userId {}: tx={}, daily={}, count={}",
                userId, saved.getMaxAmountPerTransactionRon(), saved.getMaxDailyAmountRon(), saved.getMaxDailyTransactionsCount());
        return limitMapper.toUserLimitResponseDTO(saved);
    }

    @Override
    @Transactional
    public void deleteUserLimits(Integer userId) {
        userLimitRepository.deleteByUserId(userId);
        log.info("Reset user limits for userId {}", userId);
    }

    private BankLimit getOrCreateActiveBankLimit() {
        return bankLimitRepository.findByStatus("ACTIVE").orElseGet(() -> {
            BankLimit bl = new BankLimit();
            bl.setMaxAmountPerTransactionRon(BigDecimal.valueOf(5000.0));
            bl.setMaxDailyAmountRon(BigDecimal.valueOf(20000.0));
            bl.setMaxDailyTransactionsCount(BigDecimal.valueOf(10.0));
            bl.setStatus("ACTIVE");
            bl.setCreatedAt(new Date());
            bl.setUpdatedAt(new Date());
            return bankLimitRepository.save(bl);
        });
    }
}
