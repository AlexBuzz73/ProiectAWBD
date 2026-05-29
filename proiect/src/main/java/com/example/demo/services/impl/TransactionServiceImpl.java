package com.example.demo.services.impl;

import com.example.demo.domain.*;
import com.example.demo.dto.*;
import com.example.demo.mappers.TransactionMapper;
import com.example.demo.repositories.*;
import com.example.demo.services.TransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
public class TransactionServiceImpl implements TransactionService {

    @Autowired
    private TransactionRepository transactionRepository;
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private UserLimitRepository userLimitRepository;
    @Autowired
    private BankLimitRepository bankLimitRepository;
    @Autowired
    private TagRepository tagRepository;
    @Autowired
    private ScheduledPaymentRepository scheduledPaymentRepository;
    @Autowired
    private ExchangeRateRepository exchangeRateRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private AccountAccessRepository accountAccessRepository;
    @Autowired
    private TransactionMapper transactionMapper;

    @Override
    @Transactional
    public Transaction initiatePayment(PaymentRequestDTO dto, User user) {
        if (!passwordEncoder.matches(dto.getPassword(), user.getPasswordHash())) {
            throw new RuntimeException("Parolă incorectă!");
        }

        if (dto.getProcessingType().equalsIgnoreCase("PROGRAMAT")) {
            if (dto.getScheduledDate() == null || dto.getScheduledDate().before(new Date())) {
                throw new RuntimeException("Pentru platile programate, data trebuie sa fie in viitor!");
            }
        }

        Optional<Account> destAccount = accountRepository.findByIban(dto.getDestinationIban());

        Transaction transaction = new Transaction();
        transaction.setInitiatedByUser(user);
        transaction.setAmount(dto.getAmount());
        transaction.setCurrency(dto.getCurrency());
        transaction.setDestinationIban(dto.getDestinationIban());
        transaction.setDescription(dto.getDescription());
        transaction.setStatus("DRAFT");
        transaction.setCreatedAt(new Date());
        transaction.setUpdatedAt(new Date());

        Account sourceAccount = accountRepository.findById(dto.getSourceAccountId().longValue())
                .orElseThrow(() -> new RuntimeException("Contul sursă nu a fost găsit."));
        transaction.setSourceAccount(sourceAccount);

        transaction.setCategory(categoryRepository.findById(dto.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Categoria nu exista.")));

        if (destAccount.isPresent()) {
            transaction.setDestinationAccount(destAccount.get());
            transaction.setTransactionType("INTERNAL");
        } else {
            transaction.setTransactionType("EXTERNAL");
        }

        transaction.setIsUrgent(dto.getProcessingType().equalsIgnoreCase("URGENT") ? "YES" : "NO");
        transaction.setIsScheduled(dto.getProcessingType().equalsIgnoreCase("PROGRAMAT") ? "YES" : "NO");

        Transaction savedTransaction = transactionRepository.save(transaction);

        if (dto.getTagIds() != null && !dto.getTagIds().isEmpty()) {
            for (Integer tagId : dto.getTagIds()) {
                tagRepository.findById(tagId).ifPresent(tag -> {
                    savedTransaction.getTags().add(tag);
                });
            }
            transactionRepository.save(savedTransaction);
        }

        if ("YES".equals(savedTransaction.getIsScheduled())) {
            ScheduledPayment scheduled = new ScheduledPayment();
            scheduled.setTransaction(savedTransaction);
            scheduled.setScheduledDate(dto.getScheduledDate());
            scheduled.setStatus("ACTIVE");
            scheduledPaymentRepository.save(scheduled);
        }

        return savedTransaction;
    }

    @Override
    @Transactional
    public Transaction authorizePayment(int transactionId, String password, User user) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Tranzactia nu a fost gasita"));

        if (!"DRAFT".equals(transaction.getStatus())) {
            throw new RuntimeException("Tranzactia nu este in status DRAFT!");
        }

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new RuntimeException("Autorizare esuata: Parola incorecta!");
        }

        Account sourceAccount = transaction.getSourceAccount();
        if (sourceAccount.getBalance() < transaction.getAmount()) {
            transaction.setStatus("FAILED");
            transactionRepository.save(transaction);
            throw new RuntimeException("Fonduri insuficiente!");
        }

        checkLimits(user, transaction.getAmount());
        transaction.setStatus("AUTHORIZED");
        transaction.setUpdatedAt(new Date());
        Transaction saved = transactionRepository.save(transaction);

        if ("YES".equals(saved.getIsUrgent())) {
            executeTransaction(saved.getTransactionId());
            return transactionRepository.findById(saved.getTransactionId()).get();
        }
        return saved;
    }

    private void checkLimits(User user, double amount) {
        var userLimit = userLimitRepository.findByUserUserIdAndStatus(user.getUserId(), "ACTIVE");
        double max;
        if (userLimit.isPresent()) {
            max = userLimit.get().getMaxAmountPerTransactionRon().doubleValue();
        } else {
            var bankLimit = bankLimitRepository.findAll().stream().findFirst()
                    .orElseThrow(() -> new RuntimeException("Limite bancare lipsa!"));
            max = bankLimit.getMaxAmountPerTransactionRon().doubleValue();
        }
        if (amount > max) throw new RuntimeException("Limita depasita!");
    }

    @Override
    @Transactional
    public void executeTransaction(int transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId).orElseThrow();
        Account source = transaction.getSourceAccount();
        if (!"ACTIVE".equals(source.getStatus())) throw new RuntimeException("Cont inactiv");
        if (source.getBalance() < transaction.getAmount()) throw new RuntimeException("Fara bani");

        source.setBalance(source.getBalance() - transaction.getAmount());
        accountRepository.save(source);

        if ("INTERNAL".equals(transaction.getTransactionType()) && transaction.getDestinationAccount() != null) {
            Account dest = transaction.getDestinationAccount();
            dest.setBalance(dest.getBalance() + transaction.getAmount());
            accountRepository.save(dest);
        }
        transaction.setStatus("EXECUTED");
        transaction.setUpdatedAt(new Date());
        transactionRepository.save(transaction);
    }

    @Override
    @Transactional
    public Transaction transferBetweenOwnAccounts(OwnAccountTransferDTO dto, User user) {

        if (!passwordEncoder.matches(dto.getPassword(), user.getPasswordHash())) {
            throw new RuntimeException("Parolă incorectă!");
        }

        Account source = accountRepository.findById(dto.getSourceAccountId().longValue())
                .orElseThrow(() -> new RuntimeException("Contul sursă nu a fost găsit."));

        Account destination = accountRepository.findById(dto.getDestinationAccountId().longValue())
                .orElseThrow(() -> new RuntimeException("Contul destinație nu a fost găsit."));


        if (!"ACTIVE".equals(source.getStatus()) || !"ACTIVE".equals(destination.getStatus())) {
            throw new RuntimeException("Ambele conturi trebuie să fie ACTIVE.");
        }
        if (!source.getCurrency().equals(destination.getCurrency())) {
            throw new RuntimeException("Conturile trebuie să aibă aceeași valută.");
        }
        if (source.getAccountId().equals(destination.getAccountId())) {
            throw new RuntimeException("Contul sursă și cel destinație trebuie să fie diferite.");
        }

        boolean hasFullAccess = source.getAccountAccessList().stream()
                .anyMatch(a -> a.getUser().getUserId() == user.getUserId()
                        && a.getAccessRole() != null
                        && !a.getAccessRole().equalsIgnoreCase("VIEWER"));

        if (!hasFullAccess) {
            throw new RuntimeException("Nu aveți permisiunea de a efectua transferuri (rol insuficient).");
        }

        checkLimits(user, dto.getAmount());

        if (source.getBalance() < dto.getAmount()) {
            throw new RuntimeException("Fonduri insuficiente.");
        }

        source.setBalance(source.getBalance() - dto.getAmount());
        destination.setBalance(destination.getBalance() + dto.getAmount());
        accountRepository.save(source);
        accountRepository.save(destination);

        Transaction t = new Transaction();
        t.setInitiatedByUser(user);
        t.setSourceAccount(source);
        t.setDestinationAccount(destination);
        t.setDestinationIban(destination.getIban());
        t.setAmount(dto.getAmount());
        t.setCurrency(source.getCurrency());
        t.setCategory(categoryRepository.findById(dto.getCategoryId()).orElseThrow());
        t.setDescription(dto.getDescription());
        t.setStatus("EXECUTED");
        t.setTransactionType("INTERNAL");
        t.setCreatedAt(new Date());
        t.setUpdatedAt(new Date());

        return transactionRepository.save(t);
    }
    @Override
    @Transactional
    public Transaction performCurrencyExchange(CurrencyExchangeDTO dto, User user) {

        if (!passwordEncoder.matches(dto.getPassword(), user.getPasswordHash())) {
            throw new RuntimeException("Parolă incorectă!");
        }

        Account source = accountRepository.findById(dto.getSourceAccountId().longValue())
                .orElseThrow(() -> new RuntimeException("Cont sursă inexistent."));
        Account destination = accountRepository.findById(dto.getDestinationAccountId().longValue())
                .orElseThrow(() -> new RuntimeException("Cont destinație inexistent."));

        if (source.getAccountId().equals(destination.getAccountId())) {
            throw new RuntimeException("Conturile trebuie să fie diferite.");
        }
        if (source.getCurrency().equals(destination.getCurrency())) {
            throw new RuntimeException("Valutele trebuie să fie diferite pentru schimb valutar.");
        }
        if (!"ACTIVE".equals(source.getStatus()) || !"ACTIVE".equals(destination.getStatus())) {
            throw new RuntimeException("Conturile trebuie să fie ACTIVE.");
        }


        boolean hasAccess = source.getAccountAccessList().stream()
                .anyMatch(a -> a.getUser().getUserId() == user.getUserId() && !"VIEWER".equalsIgnoreCase(a.getAccessRole()));
        if (!hasAccess) throw new RuntimeException("Acces refuzat.");


        String baseCurrency = "";
        if (source.getCurrency().equals("RON")) {
            baseCurrency = destination.getCurrency(); // Perechea va fi EUR->RON sau USD->RON
        } else if (destination.getCurrency().equals("RON")) {
            baseCurrency = source.getCurrency();
        } else {
            throw new RuntimeException("Sistemul permite doar schimburi care implică RON (RON-EUR sau RON-USD).");
        }

        final String finalBaseCurrency = source.getCurrency().equals("RON")
                ? destination.getCurrency()
                : source.getCurrency();
        ExchangeRate rateEntity = exchangeRateRepository.findByCurrencyFromAndCurrencyTo(finalBaseCurrency, "RON")
                .orElseThrow(() -> new RuntimeException("Cursul valutar pentru " + finalBaseCurrency + " nu a fost găsit."));

        double rate = rateEntity.getRate();
        double amountConverted;

        if (destination.getCurrency().equals("RON")) {
            amountConverted = dto.getAmount() * rate; // Din Valută în RON
        } else {
            amountConverted = dto.getAmount() / rate; // Din RON în Valută
        }

        checkLimits(user, dto.getAmount());
        if (source.getBalance() < dto.getAmount()) throw new RuntimeException("Fonduri insuficiente.");

        source.setBalance(source.getBalance() - dto.getAmount());
        destination.setBalance(destination.getBalance() + amountConverted);
        accountRepository.save(source);
        accountRepository.save(destination);


        Transaction t = new Transaction();
        t.setInitiatedByUser(user);
        t.setSourceAccount(source);
        t.setDestinationAccount(destination);
        t.setAmount(dto.getAmount()); // Suma originală
        t.setCurrency(source.getCurrency());
        t.setExchangeRate(rateEntity); // Referință către rata de bază
        t.setCategory(categoryRepository.findById(dto.getCategoryId()).orElseThrow());
        t.setDescription(dto.getDescription());
        t.setStatus("EXECUTED");
        t.setTransactionType("EXCHANGE");
        t.setCreatedAt(new Date());

        return transactionRepository.save(t);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponseDTO<TransactionSummaryDTO> getTransactionsForUserPaged(int userId, int page, int size, String sortBy, String direction) {
        validateActiveUser(userId);

        Pageable pageable = PageRequest.of(page, size, createTransactionSort(sortBy, direction));

        Page<Transaction> transactionPage = transactionRepository.findTransactionsForUserAccounts(userId, pageable);

        List<TransactionSummaryDTO> transactions = transactionPage.getContent()
                .stream()
                .map(transaction -> transactionMapper.toTransactionSummaryDTO(transaction))
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
        validateActiveUser(userId);
        validateUserAccessToAccount(accountId, userId);

        Pageable pageable = PageRequest.of(page, size, createTransactionSort(sortBy, direction));

        Page<Transaction> transactionPage = transactionRepository.findTransactionsForAccount(accountId, pageable);

        List<TransactionSummaryDTO> transactions = transactionPage.getContent()
                .stream()
                .map(transaction -> transactionMapper.toTransactionSummaryDTO(transaction))
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

    private void validateActiveUser(int userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found!"));

        if (!"ACTIVE".equals(user.getStatus())) {
            throw new IllegalArgumentException("User is not active!");
        }
    }

    private void validateUserAccessToAccount(Long accountId, int userId) {
        AccountAccess accountAccess = accountAccessRepository
                .findByAccountAccountIdAndUserUserIdAndStatus(accountId, userId, "ACTIVE")
                .orElseThrow(() -> new IllegalArgumentException("You don't have access to this account!"));

        if (!"ACTIVE".equals(accountAccess.getAccount().getStatus())) {
            throw new IllegalArgumentException("Account is not active!");
        }
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