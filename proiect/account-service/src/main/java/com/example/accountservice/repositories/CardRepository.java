package com.example.accountservice.repositories;

import com.example.accountservice.domain.Card;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CardRepository extends JpaRepository<Card, Integer> {

    boolean existsCardByAccountAccountId(Long accountId);

    boolean existsCardByAccountAccountIdAndStatus(Long accountId, String status);

    boolean existsByCardNumber(String cardNumber);

    Optional<Card> findFirstByAccountAccountIdAndStatus(Long accountId, String status);

    Optional<Card> findFirstByAccountAccountId(Long accountId);

    List<Card> findByAccountAccountId(Long accountId);

    Optional<Card> findByAccountAccountIdAndCardId(Long accountId, Integer cardId);
}
