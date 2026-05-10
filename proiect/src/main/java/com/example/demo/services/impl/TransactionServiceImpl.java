package com.example.demo.services.impl;

import com.example.demo.domain.*;
import com.example.demo.dto.PaymentRequestDTO;
import com.example.demo.repositories.*;
import com.example.demo.services.TransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
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

    @Override
    @Transactional
    public Transaction initiatePayment(PaymentRequestDTO dto, User user) {

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
                    // Adaugam in colectia de tags a entitatii
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
        if (!password.equals(user.getPasswordHash())) {
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
}