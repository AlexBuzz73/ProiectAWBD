package com.example.demo.services.impl;

import com.example.demo.domain.*;
import com.example.demo.dto.*;
import com.example.demo.repositories.*;
import com.example.demo.services.AdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final AccountAccessRepository accountAccessRepository;
    private final BankLimitRepository bankLimitRepository;

    @Override
    @Transactional
    public Account createSharedAccount(SharedAccountRequest dto) {
        if (dto.getUsers().size() > 2) {
            throw new IllegalArgumentException("Un cont partajat poate avea maxim 2 utilizatori.");
        }

        boolean hasOwner = dto.getUsers().stream()
                .anyMatch(u -> "OWNER".equalsIgnoreCase(u.getRole()));
        if (!hasOwner) {
            throw new IllegalArgumentException("Contul trebuie să aibă cel puțin un utilizator cu rolul OWNER.");
        }


        Account account = new Account();
        account.setAlias(dto.getAlias());
        account.setCurrency(dto.getCurrency());
        account.setBalance(0.0);
        account.setStatus("ACTIVE");
        account.setIban(generateUniqueIban());
        account.setCreatedAt(new Date());
        Account savedAccount = accountRepository.save(account);


        for (UserRoleDTO userDto : dto.getUsers()) {
            User user = userRepository.findByEmail(userDto.getEmail())
                    .orElseThrow(() -> new IllegalArgumentException("Utilizatorul " + userDto.getEmail() + " nu există."));

            AccountAccess access = new AccountAccess();
            access.setAccount(savedAccount);
            access.setUser(user);
            access.setAccessRole(userDto.getRole().toUpperCase());
            access.setStatus("ACTIVE");
            access.setCreatedAt(new Date());
            accountAccessRepository.save(access);
        }

        log.info("Admin: cont partajat creat - accountId={}, alias={}, utilizatori={}",
                savedAccount.getAccountId(), savedAccount.getAlias(), dto.getUsers().size());

        return savedAccount;
    }

    @Override
    @Transactional
    public void updateBankLimits(BankLimitUpdateDTO dto) {

        BankLimit limits = bankLimitRepository.findAll().stream().findFirst()
                .orElse(new BankLimit());

        limits.setMaxAmountPerTransactionRon(dto.getMaxAmountPerTransactionRon());
        limits.setMaxDailyAmountRon(dto.getMaxDailyAmountRon());

        limits.setMaxDailyTransactionsCount(java.math.BigDecimal.valueOf(dto.getMaxDailyTransactionsCount()));
        limits.setUpdatedAt(new Date());

        bankLimitRepository.save(limits);
        log.info("Admin: limite bancare globale actualizate - maxPerTranzactie={}, maxZilnic={}",
                dto.getMaxAmountPerTransactionRon(), dto.getMaxDailyAmountRon());
    }

    @Override
    @Transactional
    public void revokeAccountAccess(Long accountId, String email) {
        AccountAccess access = accountAccessRepository.findByAccountAccountIdAndUserEmail(accountId, email)
                .orElseThrow(() -> new IllegalArgumentException("Nu s-a găsit permisiunea de acces pentru acest cont și email."));


        if ("OWNER".equals(access.getAccessRole())) {
            long activeOwners = accountAccessRepository.findByAccountAccountId(accountId).stream()
                    .filter(a -> "OWNER".equals(a.getAccessRole()) && "ACTIVE".equals(a.getStatus()))
                    .count();
            if (activeOwners <= 1) {
                log.warn("Admin: revocare acces refuzata - {} ar ramane fara niciun OWNER activ (accountId={})", email, accountId);
                throw new IllegalArgumentException("Operațiune refuzată: Trebuie să rămână cel puțin un OWNER activ pe cont.");
            }
        }

        access.setStatus("INACTIVE");
        access.setUpdatedAt(new Date());
        accountAccessRepository.save(access);
        log.info("Admin: acces revocat - accountId={}, email={}", accountId, email);
    }

    private String generateUniqueIban() {
        String iban;

        do {
            String accountNumber = String.format("%016d", Math.abs(new java.util.Random().nextLong()) % 1_000_000_000_000_0000L);
            iban = "RO11BANK" + accountNumber;
        } while (accountRepository.existsByIban(iban));

        return iban;
    }
}
