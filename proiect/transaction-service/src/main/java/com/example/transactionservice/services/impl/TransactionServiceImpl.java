package com.example.transactionservice.services.impl;

import com.example.transactionservice.client.AccountClient;
import com.example.transactionservice.domain.*;
import com.example.transactionservice.dto.*;
import com.example.transactionservice.mappers.TransactionMapper;
import com.example.transactionservice.repositories.*;
import com.example.transactionservice.services.TransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;
    private final TagRepository tagRepository;
    private final ScheduledPaymentRepository scheduledPaymentRepository;
    private final ExchangeRateRepository exchangeRateRepository;
    private final AccountClient accountClient;
    private final TransactionMapper transactionMapper;

    @Override
    @Transactional
    public Transaction initiatePayment(PaymentRequestDTO dto, Integer userId) {
        log.debug("Initiere plata: userId={}, sourceAccountId={}, suma={}, tip={}",
                userId, dto.getSourceAccountId(), dto.getAmount(), dto.getProcessingType());

        if ("PROGRAMAT".equalsIgnoreCase(dto.getProcessingType())) {
            if (dto.getScheduledDate() == null || dto.getScheduledDate().before(new Date())) {
                throw new IllegalArgumentException("Pentru platile programate, data trebuie sa fie in viitor!");
            }
        }

        AccountInternalSummaryDTO sourceAccount = accountClient.getAccount(dto.getSourceAccountId());
        if (sourceAccount == null) {
            throw new IllegalArgumentException("Contul sursă nu a fost găsit.");
        }

        if (!"ACTIVE".equalsIgnoreCase(sourceAccount.getStatus())) {
            throw new IllegalArgumentException("Contul sursă nu este activ!");
        }

        if (!sourceAccount.getCurrency().equalsIgnoreCase(dto.getCurrency())) {
            throw new IllegalArgumentException("Valuta tranzacției trebuie să corespundă cu valuta contului sursă!");
        }

        boolean hasPayAccess = accountClient.checkAccess(dto.getSourceAccountId(), userId, "CO_OWNER");
        if (!hasPayAccess) {
            throw new IllegalArgumentException("Nu aveți acces la acest cont!");
        }

        AccountInternalSummaryDTO destAccount = accountClient.getAccountByIban(dto.getDestinationIban());

        Category category = categoryRepository.findById(dto.getCategoryId())
                .orElseThrow(() -> new IllegalArgumentException("Categoria nu exista."));

        if (!"ACTIVE".equalsIgnoreCase(category.getStatus())) {
            throw new IllegalArgumentException("Categoria nu este activa!");
        }

        if (!"Y".equalsIgnoreCase(category.getIsSystem())
                && (category.getCreatedByUserId() == null || !category.getCreatedByUserId().equals(userId))) {
            throw new IllegalArgumentException("Nu aveti acces la aceasta categorie!");
        }

        Transaction transaction = new Transaction();
        transaction.setInitiatedByUserId(userId);
        transaction.setAmount(dto.getAmount());
        transaction.setCurrency(dto.getCurrency());
        transaction.setDestinationIban(dto.getDestinationIban());
        transaction.setDescription(dto.getDescription());
        transaction.setStatus("DRAFT");
        transaction.setCreatedAt(new Date());
        transaction.setUpdatedAt(new Date());
        transaction.setSourceAccountId(sourceAccount.getAccountId());
        transaction.setSourceAccountIban(sourceAccount.getIban());
        transaction.setSourceAccountAlias(sourceAccount.getAlias());
        transaction.setCategory(category);

        if (destAccount != null) {
            transaction.setDestinationAccountId(destAccount.getAccountId());
            transaction.setDestinationAccountIban(destAccount.getIban());
            transaction.setDestinationAccountAlias(destAccount.getAlias());
            transaction.setTransactionType("INTERNAL");
        } else {
            transaction.setTransactionType("EXTERNAL");
        }

        transaction.setIsUrgent("URGENT".equalsIgnoreCase(dto.getProcessingType()) ? "YES" : "NO");
        transaction.setIsScheduled("PROGRAMAT".equalsIgnoreCase(dto.getProcessingType()) ? "YES" : "NO");

        Transaction savedTransaction = transactionRepository.save(transaction);

        if (dto.getTagIds() != null && !dto.getTagIds().isEmpty()) {
            for (Integer tagId : dto.getTagIds()) {
                tagRepository.findById(tagId).ifPresent(tag -> savedTransaction.getTags().add(tag));
            }
            transactionRepository.save(savedTransaction);
        }

        if ("YES".equals(savedTransaction.getIsScheduled())) {
            ScheduledPayment scheduled = new ScheduledPayment();
            scheduled.setTransaction(savedTransaction);
            scheduled.setScheduledDate(dto.getScheduledDate());
            scheduled.setStatus("ACTIVE");
            scheduled.setCreatedAt(new Date());
            scheduled.setUpdatedAt(new Date());
            scheduledPaymentRepository.save(scheduled);
        }

        return authorizePayment(savedTransaction.getTransactionId(), dto.getPassword(), userId);
    }

    @Override
    @Transactional
    public Transaction authorizePayment(Long transactionId, String password, Integer userId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("Tranzactia nu a fost gasita"));

        if (!"DRAFT".equals(transaction.getStatus())) {
            throw new IllegalArgumentException("Tranzactia nu este in status DRAFT!");
        }

        AccountInternalSummaryDTO sourceAccount = accountClient.getAccount(transaction.getSourceAccountId());
        if (sourceAccount.getBalance().compareTo(transaction.getAmount()) < 0) {
            transaction.setStatus("FAILED");
            transaction.setUpdatedAt(new Date());
            transactionRepository.save(transaction);
            log.warn("Autorizare esuata (fonduri insuficiente): transactionId={}, sold={}, suma={}",
                    transactionId, sourceAccount.getBalance(), transaction.getAmount());
            throw new IllegalArgumentException("Fonduri insuficiente!");
        }

        checkLimits(userId, transaction.getAmount());
        transaction.setUpdatedAt(new Date());

        if ("YES".equals(transaction.getIsUrgent())) {
            transaction.setStatus("AUTHORIZED");
            Transaction saved = transactionRepository.save(transaction);
            executeTransaction(saved.getTransactionId());
            log.info("Plata urgenta {} autorizata si executata imediat.", transactionId);
            return transactionRepository.findById(saved.getTransactionId()).get();
        }

        transaction.setStatus("PENDING_EXECUTION");
        log.info("Plata {} autorizata, in asteptare de executie (PENDING_EXECUTION).", transactionId);
        return transactionRepository.save(transaction);
    }

    private void checkLimits(Integer userId, BigDecimal amount) {
        UserLimitResponseDTO userLimit = accountClient.getUserLimits(userId);
        if (userLimit != null && userLimit.getMaxAmountPerTransactionRon() != null) {
            if (amount.compareTo(userLimit.getMaxAmountPerTransactionRon()) > 0) {
                throw new IllegalArgumentException("Limita depasita!");
            }
        }
    }

    @Override
    @Transactional
    public void executeTransaction(Long transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("Tranzacția nu a fost găsită."));

        AccountInternalSummaryDTO source = accountClient.getAccount(transaction.getSourceAccountId());
        if (!"ACTIVE".equalsIgnoreCase(source.getStatus()) || source.getBalance().compareTo(transaction.getAmount()) < 0) {
            transaction.setStatus("FAILED");
            transaction.setUpdatedAt(new Date());
            transactionRepository.save(transaction);
            log.warn("Executie esuata pentru tranzactia {}: {}", transactionId,
                    !"ACTIVE".equalsIgnoreCase(source.getStatus()) ? "cont inactiv" : "fonduri insuficiente");
            throw new IllegalArgumentException(!"ACTIVE".equalsIgnoreCase(source.getStatus()) ? "Cont inactiv" : "Fara bani");
        }

        accountClient.debit(transaction.getSourceAccountId(), transaction.getAmount(), "TX-" + transactionId);

        if ("INTERNAL".equals(transaction.getTransactionType()) && transaction.getDestinationAccountId() != null) {
            accountClient.credit(transaction.getDestinationAccountId(), transaction.getAmount(), "TX-" + transactionId);
        }

        transaction.setStatus("EXECUTED");
        transaction.setUpdatedAt(new Date());
        transactionRepository.save(transaction);
        log.info("Tranzactia {} executata cu succes (suma={}).", transactionId, transaction.getAmount());
    }

    @Override
    @Transactional
    public Transaction transferBetweenOwnAccounts(OwnAccountTransferDTO dto, Integer userId) {
        AccountInternalSummaryDTO source = accountClient.getAccount(dto.getSourceAccountId());
        AccountInternalSummaryDTO destination = accountClient.getAccount(dto.getDestinationAccountId());

        if (source == null || destination == null) {
            throw new IllegalArgumentException("Contul nu a fost găsit.");
        }

        if (!"ACTIVE".equalsIgnoreCase(source.getStatus()) || !"ACTIVE".equalsIgnoreCase(destination.getStatus())) {
            throw new IllegalArgumentException("Ambele conturi trebuie să fie ACTIVE.");
        }
        if (!source.getCurrency().equalsIgnoreCase(destination.getCurrency())) {
            throw new IllegalArgumentException("Conturile trebuie să aibă aceeași valută.");
        }
        if (source.getAccountId().equals(destination.getAccountId())) {
            throw new IllegalArgumentException("Contul sursă și cel destinație trebuie să fie diferite.");
        }

        boolean hasFullAccess = accountClient.checkAccess(source.getAccountId(), userId, "CO_OWNER");
        if (!hasFullAccess) {
            throw new IllegalArgumentException("Nu aveți permisiunea de a efectua transferuri (rol insuficient).");
        }

        boolean ownsDestination = accountClient.checkAccess(destination.getAccountId(), userId, "VIEWER");
        if (!ownsDestination) {
            throw new IllegalArgumentException("Contul destinație trebuie să vă aparțină (transfer doar între conturile proprii).");
        }

        checkLimits(userId, dto.getAmount());

        if (source.getBalance().compareTo(dto.getAmount()) < 0) {
            throw new IllegalArgumentException("Fonduri insuficiente.");
        }

        accountClient.debit(source.getAccountId(), dto.getAmount(), "OWN-DEBIT");
        accountClient.credit(destination.getAccountId(), dto.getAmount(), "OWN-CREDIT");

        Transaction t = new Transaction();
        t.setInitiatedByUserId(userId);
        t.setSourceAccountId(source.getAccountId());
        t.setSourceAccountIban(source.getIban());
        t.setSourceAccountAlias(source.getAlias());
        t.setDestinationAccountId(destination.getAccountId());
        t.setDestinationAccountIban(destination.getIban());
        t.setDestinationAccountAlias(destination.getAlias());
        t.setDestinationIban(destination.getIban());
        t.setAmount(dto.getAmount());
        t.setCurrency(source.getCurrency());
        t.setCategory(categoryRepository.findById(dto.getCategoryId())
                .orElseThrow(() -> new IllegalArgumentException("Categoria nu există.")));
        t.setDescription(dto.getDescription());
        t.setStatus("EXECUTED");
        t.setTransactionType("INTERNAL");
        t.setIsUrgent("NO");
        t.setIsScheduled("NO");
        t.setCreatedAt(new Date());
        t.setUpdatedAt(new Date());

        log.info("Transfer intre conturi proprii reusit: userId={}, din contul {} in contul {}, suma={}",
                userId, source.getAccountId(), destination.getAccountId(), dto.getAmount());

        return transactionRepository.save(t);
    }

    @Override
    @Transactional
    public Transaction performCurrencyExchange(CurrencyExchangeDTO dto, Integer userId) {
        AccountInternalSummaryDTO source = accountClient.getAccount(dto.getSourceAccountId());
        AccountInternalSummaryDTO destination = accountClient.getAccount(dto.getDestinationAccountId());

        if (source == null || destination == null) {
            throw new IllegalArgumentException("Cont inexistent.");
        }

        if (source.getAccountId().equals(destination.getAccountId())) {
            throw new IllegalArgumentException("Conturile trebuie să fie diferite.");
        }
        if (source.getCurrency().equalsIgnoreCase(destination.getCurrency())) {
            throw new IllegalArgumentException("Valutele trebuie să fie diferite pentru schimb valutar.");
        }
        if (!"ACTIVE".equalsIgnoreCase(source.getStatus()) || !"ACTIVE".equalsIgnoreCase(destination.getStatus())) {
            throw new IllegalArgumentException("Conturile trebuie să fie ACTIVE.");
        }

        boolean hasAccess = accountClient.checkAccess(source.getAccountId(), userId, "CO_OWNER");
        if (!hasAccess) throw new IllegalArgumentException("Acces refuzat.");

        boolean ownsDestination = accountClient.checkAccess(destination.getAccountId(), userId, "VIEWER");
        if (!ownsDestination) {
            throw new IllegalArgumentException("Contul destinație trebuie să vă aparțină (schimb valutar doar între conturile proprii).");
        }

        if (!source.getCurrency().equalsIgnoreCase("RON") && !destination.getCurrency().equalsIgnoreCase("RON")) {
            throw new IllegalArgumentException("Sistemul permite doar schimburi care implică RON (RON-EUR sau RON-USD).");
        }

        final String baseCurrency = source.getCurrency().equalsIgnoreCase("RON")
                ? destination.getCurrency().toUpperCase()
                : source.getCurrency().toUpperCase();

        ExchangeRate rateEntity = exchangeRateRepository.findTopByCurrencyFromAndCurrencyToOrderByRateDateDesc(baseCurrency, "RON")
                .orElseThrow(() -> new IllegalArgumentException("Cursul valutar pentru " + baseCurrency + " nu a fost găsit."));

        BigDecimal rate = rateEntity.getRate();
        BigDecimal amountConverted;

        if (destination.getCurrency().equalsIgnoreCase("RON")) {
            amountConverted = dto.getAmount().multiply(rate).setScale(2, RoundingMode.HALF_UP);
        } else {
            amountConverted = dto.getAmount().divide(rate, 2, RoundingMode.HALF_UP);
        }

        checkLimits(userId, dto.getAmount());
        if (source.getBalance().compareTo(dto.getAmount()) < 0) {
            throw new IllegalArgumentException("Fonduri insuficiente.");
        }

        accountClient.debit(source.getAccountId(), dto.getAmount(), "EXCHANGE-DEBIT");
        accountClient.credit(destination.getAccountId(), amountConverted, "EXCHANGE-CREDIT");

        Transaction t = new Transaction();
        t.setInitiatedByUserId(userId);
        t.setSourceAccountId(source.getAccountId());
        t.setSourceAccountIban(source.getIban());
        t.setSourceAccountAlias(source.getAlias());
        t.setDestinationAccountId(destination.getAccountId());
        t.setDestinationAccountIban(destination.getIban());
        t.setDestinationAccountAlias(destination.getAlias());
        t.setDestinationIban(destination.getIban());
        t.setAmount(dto.getAmount());
        t.setCurrency(source.getCurrency());
        t.setExchangeRate(rateEntity);
        t.setCategory(categoryRepository.findById(dto.getCategoryId())
                .orElseThrow(() -> new IllegalArgumentException("Categoria nu există.")));
        t.setDescription(dto.getDescription());
        t.setStatus("EXECUTED");
        t.setTransactionType("EXCHANGE");
        t.setIsUrgent("NO");
        t.setIsScheduled("NO");
        t.setCreatedAt(new Date());
        t.setUpdatedAt(new Date());

        log.info("Schimb valutar reusit: userId={}, din contul {} in contul {}, suma={}, curs={}",
                userId, source.getAccountId(), destination.getAccountId(), dto.getAmount(), rate);

        return transactionRepository.save(t);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponseDTO<TransactionSummaryDTO> getTransactionsForUserPaged(int userId, int page, int size, String sortBy, String direction) {
        Pageable pageable = PageRequest.of(page, size, createTransactionSort(sortBy, direction));

        List<AccountInternalSummaryDTO> userAccounts = accountClient.getUserAccounts(userId);
        Set<Long> accountIds = userAccounts.stream().map(AccountInternalSummaryDTO::getAccountId).collect(Collectors.toSet());

        Page<Transaction> transactionPage;
        if (accountIds.isEmpty()) {
            transactionPage = transactionRepository.findByInitiatedByUserId(userId, pageable);
        } else {
            transactionPage = transactionRepository.findTransactionsForUserOrAccounts(userId, accountIds, pageable);
        }

        List<TransactionSummaryDTO> transactions = transactionPage.getContent()
                .stream()
                .map(transaction -> transactionMapper.toTransactionSummaryDTO(transaction, userId, accountIds))
                .toList();

        return new PageResponseDTO<>(
                transactions,
                transactionPage.getNumber(),
                transactionPage.getSize(),
                transactionPage.getTotalElements(),
                transactionPage.getTotalPages(),
                transactionPage.isFirst(),
                transactionPage.isLast()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponseDTO<TransactionSummaryDTO> getTransactionsForAccountPaged(Long accountId, int userId, int page, int size, String sortBy, String direction) {
        Pageable pageable = PageRequest.of(page, size, createTransactionSort(sortBy, direction));

        List<AccountInternalSummaryDTO> userAccounts = accountClient.getUserAccounts(userId);
        Set<Long> accountIds = userAccounts.stream().map(AccountInternalSummaryDTO::getAccountId).collect(Collectors.toSet());

        Page<Transaction> transactionPage = transactionRepository.findTransactionsForAccount(accountId, pageable);

        List<TransactionSummaryDTO> transactions = transactionPage.getContent()
                .stream()
                .map(transaction -> transactionMapper.toTransactionSummaryDTO(transaction, userId, accountIds))
                .toList();

        return new PageResponseDTO<>(
                transactions,
                transactionPage.getNumber(),
                transactionPage.getSize(),
                transactionPage.getTotalElements(),
                transactionPage.getTotalPages(),
                transactionPage.isFirst(),
                transactionPage.isLast()
        );
    }

    private Sort createTransactionSort(String sortBy, String direction) {
        String sortProperty = switch (sortBy) {
            case "createdAt" -> "createdAt";
            case "amount" -> "amount";
            default -> throw new IllegalArgumentException("Invalid transaction sort field!");
        };

        Sort.Direction sortDirection;
        if ("asc".equalsIgnoreCase(direction)) {
            sortDirection = Sort.Direction.ASC;
        } else if ("desc".equalsIgnoreCase(direction)) {
            sortDirection = Sort.Direction.DESC;
        } else {
            throw new IllegalArgumentException("Invalid sort direction!");
        }

        return Sort.by(sortDirection, sortProperty)
                .and(Sort.by(Sort.Direction.DESC, "transactionId"));
    }
}
